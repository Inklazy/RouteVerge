package com.inklazy.routeverge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inklazy.routeverge.ui.AppRoot
import com.inklazy.routeverge.ui.RouteVergeEvent
import com.inklazy.routeverge.ui.RouteVergeViewModel
import com.inklazy.routeverge.ui.dialogs.CurrentNfcLinkDialog
import com.inklazy.routeverge.ui.dialogs.ForceUpdateDialog
import com.inklazy.routeverge.ui.dialogs.NfcActivationDialog
import com.inklazy.routeverge.ui.dialogs.NfcLinkDialog
import com.inklazy.routeverge.ui.dialogs.StartupAgreementDialog
import com.inklazy.routeverge.ui.dialogs.UpdateCheckFailedDialog
import com.inklazy.routeverge.ui.dialogs.UpdateCheckingDialog
import com.inklazy.routeverge.ui.theme.RouteVergeTheme
import com.inklazy.routeverge.nfc.NfcLauncherController
import com.inklazy.routeverge.update.AppUpdateChecker
import com.inklazy.routeverge.update.AppUpdateResult
import kotlinx.coroutines.launch

private const val STARTUP_AGREEMENT_PREFS_NAME = "startup_agreement_prefs"
private const val KEY_STARTUP_AGREEMENT_ACCEPTED = "startup_agreement_accepted"
private const val STATE_FORCE_UPDATE_VERSION_CODE = "force_update_checked_version_code"
private const val STATE_FORCE_UPDATE_VERSION = "force_update_version"
private const val STATE_FORCE_UPDATE_URL = "force_update_url"
private const val STATE_FORCE_UPDATE_MESSAGE = "force_update_message"

internal enum class StartupPhase { CHECKING_UPDATE, UPDATE_FAILED, FORCE_UPDATE, AGREEMENT, READY }

/** Retained across configuration changes; process recreation starts a fresh gate check. */
internal class StartupGateState : ViewModel() {
    var started = false
    val phase = mutableStateOf(StartupPhase.CHECKING_UPDATE)
    val forceUpdateResult = mutableStateOf<AppUpdateResult?>(null)
}

/** Activity boundary only: system entry points, startup gates and Compose host. */
class MainActivity : ComponentActivity() {
    private val viewModel: RouteVergeViewModel by lazy {
        ViewModelProvider(this)[RouteVergeViewModel::class.java]
    }
    private val startupGate: StartupGateState by lazy {
        ViewModelProvider(this)[StartupGateState::class.java]
    }
    private lateinit var nfcLauncher: NfcLauncherController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    private var nfcActivatedState = mutableStateOf(false)
    private var nfcLinkState = mutableStateOf("")
    private val startupPhase get() = startupGate.phase
    private val forceUpdateResult get() = startupGate.forceUpdateResult
    private var nfcSubmittingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()
        nfcLauncher = NfcLauncherController(this, ::refreshNfcState)
        nfcLauncher.onCreate()
        viewModel.refresh()
        observeViewModelEvents()

        // Retain an already checked gate across rotation. After process death,
        // recheck unless a known force-update decision was saved for this version.
        if (!startupGate.started) {
            startupGate.started = true
            val savedForceUpdate = savedInstanceState?.takeIf {
                it.getInt(STATE_FORCE_UPDATE_VERSION_CODE, -1) == BuildConfig.VERSION_CODE
            }?.getString(STATE_FORCE_UPDATE_VERSION)
            if (savedForceUpdate != null) {
                forceUpdateResult.value = AppUpdateResult(
                    updateRequired = true,
                    latestVersion = savedForceUpdate,
                    releaseUrl = savedInstanceState.getString(STATE_FORCE_UPDATE_URL).orEmpty(),
                    downloadUrl = savedInstanceState.getString(STATE_FORCE_UPDATE_URL).orEmpty(),
                    message = savedInstanceState.getString(STATE_FORCE_UPDATE_MESSAGE).orEmpty()
                )
                startupPhase.value = StartupPhase.FORCE_UPDATE
            } else {
                checkForUpdates()
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            RouteVergeTheme {
                AppRoot(
                    uiState = uiState,
                    isNfcActivated = nfcActivatedState.value,
                    nfcLinkConfigured = nfcLinkState.value.isNotBlank(),
                    onMapProviderChange = viewModel::setMapProvider,
                    onShowCurrentLinkDialog = nfcLauncher::showCurrentLinkDialog,
                    onOpenProjectHome = { openExternalUrl(getString(R.string.settings_project_home_url)) },
                    onRequestPermissions = ::requestRuntimePermissions,
                    onOpenDeveloperOptions = ::openDeveloperOptions,
                    onVerifyNfc = nfcLauncher::showActivationDialog,
                    onOpenAlipayNfc = nfcLauncher::openAlipayLink,
                    onStartPoint = { lat, lng ->
                        if (nfcLauncher.ensureActivated()) viewModel.startPoint(lat, lng)
                    },
                    onStartRoute = { route, speed ->
                        if (nfcLauncher.ensureActivated()) viewModel.startRoute(route, speed)
                    },
                    onPause = viewModel::pauseMocking,
                    onResumeMock = viewModel::resumeMocking,
                    onStop = viewModel::stopMocking,
                    onDeleteRoute = viewModel::deleteRoute,
                    onSavePoint = viewModel::savePoint,
                    onUpdatePoint = viewModel::updatePoint,
                    onDeletePoint = viewModel::deletePoint,
                    onSaveRoute = viewModel::saveRoute,
                    onLocateMe = viewModel::lastKnownRoutePoint
                )
                when (startupPhase.value) {
                    StartupPhase.CHECKING_UPDATE -> UpdateCheckingDialog()
                    StartupPhase.UPDATE_FAILED -> UpdateCheckFailedDialog(
                        onRetry = ::checkForUpdates,
                        onContinue = ::continuePastUpdateFailure
                    )
                    StartupPhase.FORCE_UPDATE -> forceUpdateResult.value?.let { result ->
                        ForceUpdateDialog(
                            latestVersion = result.latestVersion,
                            message = result.message,
                            onDownload = { openExternalUrl(result.downloadUrl) },
                            onRetry = ::checkForUpdates
                        )
                    }
                    StartupPhase.AGREEMENT -> StartupAgreementDialog(
                        onAccept = ::acceptStartupAgreement,
                        onExit = { finish() }
                    )
                    StartupPhase.READY -> Unit
                }
                if (startupPhase.value == StartupPhase.READY) {
                    if (nfcLauncher.activationDialogVisible) {
                        NfcActivationDialog(
                            deviceId = nfcLauncher.deviceId,
                            isSubmitting = nfcSubmittingState.value,
                            onActivate = { code ->
                                nfcSubmittingState.value = true
                                nfcLauncher.redeemActivationCode(code) {
                                    nfcSubmittingState.value = false
                                }
                            },
                            onTelegram = nfcLauncher::openTelegramVerification
                        )
                    }
                    if (nfcLauncher.linkDialogVisible) {
                        NfcLinkDialog(
                            url = nfcLauncher.linkDialogUrl,
                            onSave = { nfcLauncher.saveDiscoveredUrl(nfcLauncher.linkDialogUrl) },
                            onCopy = { nfcLauncher.copyLink(nfcLauncher.linkDialogUrl) },
                            onDismiss = nfcLauncher::dismissLinkDialog
                        )
                    }
                    if (nfcLauncher.currentLinkDialogVisible) {
                        CurrentNfcLinkDialog(
                            url = nfcLauncher.currentLink,
                            onCopy = { nfcLauncher.copyLink(nfcLauncher.currentLink) },
                            onDismiss = nfcLauncher::dismissCurrentLinkDialog
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Preserve an already known force-update decision through process recovery;
        // a failed retry must not turn it into an optional network-failure gate.
        forceUpdateResult.value?.let { result ->
            outState.putInt(STATE_FORCE_UPDATE_VERSION_CODE, BuildConfig.VERSION_CODE)
            outState.putString(STATE_FORCE_UPDATE_VERSION, result.latestVersion)
            outState.putString(STATE_FORCE_UPDATE_URL, result.downloadUrl)
            outState.putString(STATE_FORCE_UPDATE_MESSAGE, result.message)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        nfcLauncher.onResume()
    }

    override fun onPause() {
        nfcLauncher.onPause()
        super.onPause()
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RouteVergeEvent.ShowMessage -> Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()
                        RouteVergeEvent.RequestLocationPermissions -> requestRuntimePermissions()
                        RouteVergeEvent.OpenDeveloperOptions -> openDeveloperOptions()
                    }
                }
            }
        }
    }

    private fun checkForUpdates() {
        startupPhase.value = StartupPhase.CHECKING_UPDATE
        Thread {
            try {
                val result = AppUpdateChecker.checkLatest()
                runOnUiThread {
                    if (result.updateRequired) {
                        forceUpdateResult.value = result
                        startupPhase.value = StartupPhase.FORCE_UPDATE
                    } else {
                        forceUpdateResult.value = null
                        startupPhase.value = if (isStartupAgreementAccepted()) StartupPhase.READY else StartupPhase.AGREEMENT
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("UpdateCheck", "update check failed", e)
                runOnUiThread {
                    startupPhase.value = if (forceUpdateResult.value != null) StartupPhase.FORCE_UPDATE
                        else StartupPhase.UPDATE_FAILED
                }
            }
        }.start()
    }

    private fun continuePastUpdateFailure() {
        startupPhase.value = if (isStartupAgreementAccepted()) StartupPhase.READY else StartupPhase.AGREEMENT
    }

    private fun isStartupAgreementAccepted(): Boolean =
        getSharedPreferences(STARTUP_AGREEMENT_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STARTUP_AGREEMENT_ACCEPTED, false)

    private fun acceptStartupAgreement() {
        getSharedPreferences(STARTUP_AGREEMENT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STARTUP_AGREEMENT_ACCEPTED, true).apply()
        startupPhase.value = StartupPhase.READY
    }

    private fun refreshNfcState() {
        nfcActivatedState.value = nfcLauncher.isActivated
        nfcLinkState.value = nfcLauncher.currentLink
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openDeveloperOptions() {
        runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            .onFailure { Toast.makeText(this, "无法打开开发者选项", Toast.LENGTH_SHORT).show() }
    }

    private fun openExternalUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(this, "无法打开更新页面", Toast.LENGTH_SHORT).show() }
    }
}