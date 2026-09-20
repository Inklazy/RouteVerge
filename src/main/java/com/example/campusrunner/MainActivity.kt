package com.example.campusrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.campusrunner.R
import com.example.campusrunner.data.LocationCache
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.data.RoutePoint
import com.example.campusrunner.data.RouteRepository
import com.example.campusrunner.data.PointRepository
import com.example.campusrunner.data.SavedPoint
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.data.UserSettings
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.nfc.NfcLauncherController
import com.example.campusrunner.service.MockLocationService
import com.example.campusrunner.service.MockSessionSnapshot
import com.example.campusrunner.service.MockPermission
import com.example.campusrunner.ui.AppRoot
import com.example.campusrunner.ui.RuntimeSession
import com.example.campusrunner.ui.RuntimeSessionKind
import com.example.campusrunner.ui.dialogs.CurrentNfcLinkDialog
import com.example.campusrunner.ui.dialogs.ForceUpdateDialog
import com.example.campusrunner.ui.dialogs.NfcActivationDialog
import com.example.campusrunner.ui.dialogs.NfcLinkDialog
import com.example.campusrunner.ui.dialogs.StartupAgreementDialog
import com.example.campusrunner.ui.dialogs.UpdateCheckFailedDialog
import com.example.campusrunner.ui.dialogs.UpdateCheckingDialog
import com.example.campusrunner.ui.theme.RouteVergeTheme
import com.example.campusrunner.update.AppUpdateChecker
import com.example.campusrunner.update.AppUpdateResult

private const val STARTUP_AGREEMENT_PREFS_NAME = "startup_agreement_prefs"
private const val KEY_STARTUP_AGREEMENT_ACCEPTED = "startup_agreement_accepted"

/**
 * Single serialized startup gate. Exactly one phase drives the user at a time:
 * update check -> (force update stops the flow) -> agreement -> ready for app.
 * NFC / permission gates render only after [READY].
 */
private enum class StartupPhase {
    CHECKING_UPDATE,
    UPDATE_FAILED,
    FORCE_UPDATE,
    AGREEMENT,
    READY
}

/**
 * App entry point: window / edge-to-edge setup, permission & service glue,
 * app state and the update/agreement gate. All UI lives in [AppRoot] and the
 * screen packages — this class no longer composes screens itself.
 */
class MainActivity : ComponentActivity() {
    private lateinit var repository: RouteRepository
    private lateinit var pointRepository: PointRepository
    private lateinit var userSettings: UserSettings
    private lateinit var nfcLauncher: NfcLauncherController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshState()
    }

    private var hasLocationPermissionState = mutableStateOf(false)
    private var canMockState = mutableStateOf(false)
    private var routesState = mutableStateOf<List<SavedRoute>>(emptyList())
    private var pointsState = mutableStateOf<List<SavedPoint>>(emptyList())
    private var mapProviderState = mutableStateOf(MapProvider.AUTO)
    private var serviceRunningState = mutableStateOf(false)
    private var servicePausedState = mutableStateOf(false)
    private var nfcActivatedState = mutableStateOf(false)
    private var nfcStatusState = mutableStateOf("")
    private var nfcLinkState = mutableStateOf("")
    private var startupPhase = mutableStateOf(StartupPhase.CHECKING_UPDATE)
    private var forceUpdateResult = mutableStateOf<AppUpdateResult?>(null)
    private var runtimeSessionState = mutableStateOf<RuntimeSession?>(null)
    private var nfcSubmittingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the activity in resize mode so Compose receives the reduced
        // IME window bounds. Home then hides its NFC bottom bar and scrolls
        // content without shrinking the fixed-height action controls.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()
        repository = RouteRepository(this)
        pointRepository = PointRepository(this)
        userSettings = UserSettings(this)
        nfcLauncher = NfcLauncherController(this, ::refreshNfcState)
        routesState.value = repository.getRoutes()
        pointsState.value = pointRepository.getPoints()
        mapProviderState.value = userSettings.mapProvider
        refreshState()
        nfcLauncher.onCreate()
        // Cold start runs the startup orchestration exactly once; activity
        // recreation (config change) skips the gates — the flow already passed.
        startupPhase.value =
            if (savedInstanceState != null) StartupPhase.READY else StartupPhase.CHECKING_UPDATE
        if (savedInstanceState == null) {
            checkForUpdates()
        }

        setContent {
            RouteVergeTheme {
                AppRoot(
                    hasLocationPermission = hasLocationPermissionState.value,
                    canMockLocation = canMockState.value,
                    routes = routesState.value,
                    savedPoints = pointsState.value,
                    mapProvider = mapProviderState.value,
                    onMapProviderChange = { provider ->
                        userSettings.mapProvider = provider
                        mapProviderState.value = provider
                    },
                    isServiceRunning = serviceRunningState.value,
                    isServicePaused = servicePausedState.value,
                    runtimeSession = runtimeSessionState.value,
                    isNfcActivated = nfcActivatedState.value,
                    nfcLinkConfigured = nfcLauncher.currentLink.isNotBlank(),
                    onShowCurrentLinkDialog = nfcLauncher::showCurrentLinkDialog,
                    onOpenProjectHome = { openExternalUrl(getString(R.string.settings_project_home_url)) },
                    onRequestPermissions = ::requestRuntimePermissions,
                    onOpenDeveloperOptions = ::openDeveloperOptions,
                    onRefreshState = ::refreshState,
                    onVerifyNfc = nfcLauncher::showActivationDialog,
                    onOpenAlipayNfc = nfcLauncher::openAlipayLink,
                    onStartPoint = ::startPoint,
                    onStartRoute = ::startRoute,
                    onPause = ::pauseMocking,
                    onResumeMock = ::resumeMocking,
                    onStop = ::stopMocking,
                    onDeleteRoute = ::deleteRoute,
                    onSavePoint = ::savePoint,
                    onUpdatePoint = ::updatePoint,
                    onDeletePoint = ::deletePoint,
                    onSaveRoute = ::saveRoute,
                    onLocateMe = ::lastKnownRoutePoint
                )
                when (startupPhase.value) {
                    StartupPhase.CHECKING_UPDATE -> UpdateCheckingDialog()

                    StartupPhase.UPDATE_FAILED -> UpdateCheckFailedDialog(
                        onRetry = ::checkForUpdates,
                        onContinue = ::continuePastUpdateFailure
                    )

                    StartupPhase.FORCE_UPDATE -> {
                        val result = forceUpdateResult.value
                        if (result != null) {
                            ForceUpdateDialog(
                                latestVersion = result.latestVersion,
                                message = result.message,
                                onDownload = { openExternalUrl(result.downloadUrl) },
                                onRetry = ::checkForUpdates
                            )
                        }
                    }

                    StartupPhase.AGREEMENT -> StartupAgreementDialog(
                        onAccept = ::acceptStartupAgreement,
                        onExit = { finish() }
                    )

                    StartupPhase.READY -> Unit
                }
                // NFC / link dialogs only render after the startup gates pass —
                // one gate at a time, no dialog stacking.
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
        refreshState()
        nfcLauncher.onResume()
    }

    override fun onPause() {
        nfcLauncher.onPause()
        super.onPause()
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
                runOnUiThread {
                    startupPhase.value = StartupPhase.UPDATE_FAILED
                }
            }
        }.start()
    }

    /** Update check failure is non-blocking: continue into the rest of the flow. */
    private fun continuePastUpdateFailure() {
        startupPhase.value =
            if (isStartupAgreementAccepted()) StartupPhase.READY else StartupPhase.AGREEMENT
    }

    private fun isStartupAgreementAccepted(): Boolean =
        getSharedPreferences(STARTUP_AGREEMENT_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STARTUP_AGREEMENT_ACCEPTED, false)

    private fun acceptStartupAgreement() {
        getSharedPreferences(STARTUP_AGREEMENT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STARTUP_AGREEMENT_ACCEPTED, true)
            .apply()
        startupPhase.value = StartupPhase.READY
    }

    private fun openExternalUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, "无法打开更新页面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun refreshState() {
        hasLocationPermissionState.value = hasFineLocationPermission()
        canMockState.value = hasLocationPermissionState.value && MockPermission.canUseMockLocation(this)
        routesState.value = repository.getRoutes()
        pointsState.value = pointRepository.getPoints()
        val persistedSession = MockLocationService.readSession(this)
        serviceRunningState.value = MockLocationService.isRunning || persistedSession != null
        servicePausedState.value = MockLocationService.isPaused || persistedSession?.isPaused == true
        if (persistedSession != null) {
            runtimeSessionState.value = runtimeSessionFrom(persistedSession)
        } else if (!MockLocationService.isRunning) {
            runtimeSessionState.value = null
        }
        refreshNfcState()
    }

    private fun runtimeSessionFrom(snapshot: MockSessionSnapshot): RuntimeSession? {
        return when (snapshot.kind) {
            "point" -> RuntimeSession(
                kind = RuntimeSessionKind.POINT,
                pointLat = snapshot.pointLat,
                pointLng = snapshot.pointLng
            )

            "route" -> {
                val route = routesState.value.firstOrNull { it.id == snapshot.routeId }
                RuntimeSession(
                    kind = RuntimeSessionKind.ROUTE,
                    routeId = snapshot.routeId,
                    routeName = snapshot.routeName,
                    speedMps = snapshot.speedMps,
                    closeLoop = snapshot.closeLoop,
                    loopCount = snapshot.loopCount,
                    totalDistanceMeters = route?.let {
                        RouteMath.totalDistanceMeters(it.points, closeLoop = it.closeLoop)
                    },
                    activeElapsedMillis = snapshot.activeElapsedMillis
                )
            }

            else -> null
        }
    }

    private fun refreshNfcState() {
        nfcActivatedState.value = nfcLauncher.isActivated
        nfcStatusState.value = nfcLauncher.nfcStatusText
        nfcLinkState.value = nfcLauncher.currentLink
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun openDeveloperOptions() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }.onFailure {
            Toast.makeText(this, "无法打开开发者选项", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPoint(latText: String, lngText: String) {
        val lat = latText.toDoubleOrNull()
        val lng = lngText.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            Toast.makeText(this, "请输入有效经纬度", Toast.LENGTH_SHORT).show()
            return
        }
        if (startServiceSafely(MockLocationService.pointIntent(this, lat, lng))) {
            runtimeSessionState.value = RuntimeSession(
                kind = RuntimeSessionKind.POINT,
                pointLat = lat,
                pointLng = lng
            )
        }
    }

    private fun startRoute(route: SavedRoute, speedText: String) {
        if (route.points.size < 2) {
            Toast.makeText(this, "路线至少需要 2 个点", Toast.LENGTH_SHORT).show()
            return
        }
        val speed = speedText.toDoubleOrNull()
        if (speed == null || speed <= 0.0) {
            Toast.makeText(this, "速度必须大于 0", Toast.LENGTH_SHORT).show()
            return
        }
        if (startServiceSafely(
            MockLocationService.routeIntent(
                context = this,
                route = route,
                speedMps = speed,
                closeLoop = route.closeLoop,
                loopCount = route.loopCount
            )
        )) {
            runtimeSessionState.value = RuntimeSession(
                kind = RuntimeSessionKind.ROUTE,
                routeId = route.id,
                routeName = route.name,
                speedMps = speed,
                closeLoop = route.closeLoop,
                loopCount = route.loopCount,
                totalDistanceMeters = RouteMath.totalDistanceMeters(route.points, closeLoop = route.closeLoop)
            )
        }
    }

    private fun pauseMocking() {
        if (!serviceRunningState.value || servicePausedState.value) return
        startService(MockLocationService.pauseIntent(this))
        servicePausedState.value = true
    }

    private fun resumeMocking() {
        if (!serviceRunningState.value || !servicePausedState.value) return
        startService(MockLocationService.resumeIntent(this))
        servicePausedState.value = false
    }

    private fun stopMocking() {
        if (!serviceRunningState.value && !servicePausedState.value) return
        startService(MockLocationService.stopIntent(this))
        serviceRunningState.value = false
        servicePausedState.value = false
        runtimeSessionState.value = null
    }

    private fun startServiceSafely(intent: Intent): Boolean {
        // The UI changes immediately after this call. Guarding the activity
        // state here also covers two rapid taps before Compose draws that frame.
        if (serviceRunningState.value || servicePausedState.value) return false
        if (!nfcLauncher.ensureActivated()) {
            return false
        }
        if (!hasLocationPermissionState.value) {
            requestRuntimePermissions()
            return false
        }
        if (!canMockState.value) {
            Toast.makeText(this, "请先在开发者选项中选择本应用为模拟位置应用", Toast.LENGTH_LONG).show()
            openDeveloperOptions()
            return false
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            serviceRunningState.value = true
            servicePausedState.value = false
            return true
        } catch (e: SecurityException) {
            Toast.makeText(this, "没有模拟位置权限，请重新选择模拟位置应用", Toast.LENGTH_LONG).show()
            openDeveloperOptions()
            return false
        }
    }

    private fun saveRoute(
        name: String,
        points: List<RoutePoint>,
        closeLoop: Boolean,
        loopCount: Int
    ): SavedRoute? {
        if (name.isBlank()) {
            Toast.makeText(this, "请输入路线名称", Toast.LENGTH_SHORT).show()
            return null
        }
        if (points.size < 2) {
            Toast.makeText(this, "路线至少需要 2 个点", Toast.LENGTH_SHORT).show()
            return null
        }
        val route = repository.saveRoute(name, points, closeLoop, loopCount)
        routesState.value = repository.getRoutes()
        Toast.makeText(this, "路线已保存", Toast.LENGTH_SHORT).show()
        return route
    }

    private fun deleteRoute(route: SavedRoute) {
        repository.deleteRoute(route.id)
        routesState.value = repository.getRoutes()
    }

    private fun savePoint(name: String?, latText: String, lngText: String): Boolean {
        val lat = latText.toDoubleOrNull(); val lng = lngText.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            Toast.makeText(this, "请输入有效经纬度", Toast.LENGTH_SHORT).show(); return false
        }
        pointRepository.savePoint(name, RoutePoint(lat, lng))
        pointsState.value = pointRepository.getPoints()
        Toast.makeText(this, "点位已保存", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun updatePoint(id: String, name: String?, latText: String, lngText: String): Boolean {
        val lat = latText.toDoubleOrNull(); val lng = lngText.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            Toast.makeText(this, "请输入有效经纬度", Toast.LENGTH_SHORT).show(); return false
        }
        if (pointRepository.updatePoint(id, name, RoutePoint(lat, lng)) == null) return false
        pointsState.value = pointRepository.getPoints()
        Toast.makeText(this, "点位已更新", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun deletePoint(id: String) {
        if (pointRepository.deletePoint(id)) pointsState.value = pointRepository.getPoints()
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownRoutePoint(): RoutePoint? {
        if (!hasFineLocationPermission()) {
            requestRuntimePermissions()
            return null
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providerNames = runCatching { locationManager.allProviders }.getOrDefault(
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        )
        val location = providerNames
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            .maxByOrNull(Location::getTime)

        if (location != null) {
            val point = RoutePoint(location.latitude, location.longitude)
            LocationCache.save(this, point)
            return point
        }

        val cachedPoint = LocationCache.read(this)
        if (cachedPoint != null) {
            Toast.makeText(this, "使用上次记录的位置定位地图", Toast.LENGTH_SHORT).show()
            return cachedPoint
        }

        Toast.makeText(this, "暂时没有当前位置，请先打开系统定位或稍后再试", Toast.LENGTH_SHORT).show()
        return null
    }
}
