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

private enum class StartupPhase { CHECKING_UPDATE, UPDATE_FAILED, FORCE_UPDATE, AGREEMENT, READY }

/** Activity boundary only: system entry points, startup gates and Compose host. */
class MainActivity : ComponentActivity() {
    private val viewModel: RouteVergeViewModel by lazy {
        ViewModelProvider(this)[RouteVergeViewModel::class.java]
    }
    private lateinit var nfcLauncher: NfcLauncherController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    private var nfcActivatedState = mutableStateOf(false)
    private var nfcLinkState = mutableStateOf("")
    private var startupPhase = mutableStateOf(StartupPhase.CHECKING_UPDATE)
    private var forceUpdateResult = mutableStateOf<AppUpdateResult?>(null)
    private var nfcSubmittingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()
        nfcLauncher = NfcLauncherController(this, ::refreshNfcState)
        nfcLauncher.onCreate()
        viewModel.refresh()
        observeViewModelEvents()

        startupPhase.value = if (savedInstanceState != null) StartupPhase.READY else StartupPhase.CHECKING_UPDATE
        if (savedInstanceState == null) checkForUpdates()

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
                    } else if (isStartupAgreementAccepted()) {
                        startupPhase.value = StartupPhase.READY
                    } else {
                        startupPhase.value = StartupPhase.AGREEMENT
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("UpdateCheck", "update check failed", e)
                runOnUiThread { startupPhase.value = StartupPhase.UPDATE_FAILED }
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