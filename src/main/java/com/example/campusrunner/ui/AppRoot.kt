package com.example.campusrunner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.data.RoutePoint
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.data.SpeedPreset
import com.example.campusrunner.ui.components.RouteVergeStatus
import com.example.campusrunner.ui.navigation.AppDestination
import com.example.campusrunner.ui.screens.home.HomeScreen
import com.example.campusrunner.ui.screens.route.MapPointPickerScreen
import com.example.campusrunner.ui.screens.settings.SettingsScreen
import com.example.campusrunner.ui.screens.route.RouteEditorScreen
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * App composition root: owns navigation state (which destination is shown),
 * the back handling and the light cross-screen UI state (route editor draft,
 * point picker draft, speed). It receives all app state + actions from the
 * Activity — no ViewModel, state source is unchanged.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppRoot(
    hasLocationPermission: Boolean,
    canMockLocation: Boolean,
    routes: List<SavedRoute>,
    mapProvider: MapProvider,
    onMapProviderChange: (MapProvider) -> Unit,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    runtimeSession: RuntimeSession?,
    isNfcActivated: Boolean,
    nfcLinkConfigured: Boolean,
    onShowCurrentLinkDialog: () -> Unit,
    onOpenProjectHome: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onRefreshState: () -> Unit,
    onVerifyNfc: () -> Unit,
    onOpenAlipayNfc: () -> Unit,
    onStartPoint: (String, String) -> Unit,
    onStartRoute: (SavedRoute, String) -> Unit,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit,
    onSaveRoute: (String, List<RoutePoint>, Boolean, Int) -> SavedRoute?,
    onLocateMe: () -> RoutePoint?
) {
    var screen by remember { mutableStateOf(AppDestination.HOME) }
    var editingRoute by remember { mutableStateOf<SavedRoute?>(null) }
    var speedText by remember { mutableStateOf(formatNumber(SpeedPreset.FAST_PACE.speedMps)) }
    var closeLoop by remember { mutableStateOf(false) }
    var loopCountText by remember { mutableStateOf("1") }
    var pointLatInput by remember { mutableStateOf("39.904200") }
    var pointLngInput by remember { mutableStateOf("116.407400") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200L)
            onRefreshState()
        }
    }

    BackHandler(enabled = screen != AppDestination.HOME) {
        screen = AppDestination.HOME
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState == AppDestination.HOME) {
                slideInHorizontally { -it / 4 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            } else {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 4 } + fadeOut()
            }
        },
        label = "screen_transition"
    ) { target ->
        when (target) {
            AppDestination.HOME -> HomeScreen(
                hasLocationPermission = hasLocationPermission,
                canMockLocation = canMockLocation,
                routes = routes,
                isServiceRunning = isServiceRunning,
                isServicePaused = isServicePaused,
                runtimeSession = runtimeSession,
                isNfcActivated = isNfcActivated,
                speedText = speedText,
                pointLatInput = pointLatInput,
                pointLngInput = pointLngInput,
                onPointLatChange = { pointLatInput = it },
                onPointLngChange = { pointLngInput = it },
                onSpeedTextChange = { speedText = it },
                onRequestPermissions = onRequestPermissions,
                onOpenDeveloperOptions = onOpenDeveloperOptions,
                onVerifyNfc = onVerifyNfc,
                onOpenAlipayNfc = onOpenAlipayNfc,
                onOpenSettings = { screen = AppDestination.SETTINGS },
                onOpenPointPicker = { screen = AppDestination.POINT_PICKER },
                onOpenRouteEditor = {
                    editingRoute = null
                    closeLoop = false
                    loopCountText = "1"
                    screen = AppDestination.ROUTE_EDITOR
                },
                onEditRoute = {
                    editingRoute = it
                    closeLoop = it.closeLoop
                    loopCountText = it.loopCount.toString()
                    screen = AppDestination.ROUTE_EDITOR
                },
                onStartPoint = { onStartPoint(pointLatInput, pointLngInput) },
                onStartRoute = onStartRoute,
                onPause = onPause,
                onResumeMock = onResumeMock,
                onStop = onStop,
                onDeleteRoute = onDeleteRoute
            )

            AppDestination.SETTINGS -> SettingsScreen(
                isNfcActivated = isNfcActivated,
                nfcLinkConfigured = nfcLinkConfigured,
                onBack = { screen = AppDestination.HOME },
                onVerifyNfc = onVerifyNfc,
                onShowCurrentLink = onShowCurrentLinkDialog,
                onOpenProjectHome = onOpenProjectHome
            )

            AppDestination.POINT_PICKER -> MapPointPickerScreen(
                mapProvider = mapProvider,
                onMapProviderChange = onMapProviderChange,
                runtimeEntryText = runtimeSummary(isServiceRunning, isServicePaused, runtimeSession),
                runtimeEntryStatus = when {
                    isServicePaused -> RouteVergeStatus.Paused
                    isServiceRunning -> RouteVergeStatus.Running
                    else -> null
                },
                onRuntimeEntryClick = { screen = AppDestination.HOME },
                startPoint = pointLatInput.toDoubleOrNull()?.let { lat ->
                    pointLngInput.toDoubleOrNull()?.let { lng -> RoutePoint(lat, lng) }
                },
                onBack = { screen = AppDestination.HOME },
                onLocateMe = onLocateMe,
                onPointPicked = { point ->
                    pointLatInput = String.format(Locale.US, "%.6f", point.latWgs84)
                    pointLngInput = String.format(Locale.US, "%.6f", point.lngWgs84)
                    screen = AppDestination.HOME
                }
            )

            AppDestination.ROUTE_EDITOR -> RouteEditorScreen(
                mapProvider = mapProvider,
                onMapProviderChange = onMapProviderChange,
                runtimeEntryText = runtimeSummary(isServiceRunning, isServicePaused, runtimeSession),
                runtimeEntryStatus = when {
                    isServicePaused -> RouteVergeStatus.Paused
                    isServiceRunning -> RouteVergeStatus.Running
                    else -> null
                },
                onRuntimeEntryClick = { screen = AppDestination.HOME },
                initialRoute = editingRoute,
                closeLoop = closeLoop,
                loopCountText = loopCountText,
                onCloseLoopChange = { closeLoop = it },
                onLoopCountTextChange = { loopCountText = it },
                onBack = { screen = AppDestination.HOME },
                onLocateMe = onLocateMe,
                onSaveRoute = { name, points ->
                    val loopCount = loopCountText.toIntOrNull() ?: 1
                    val saved = onSaveRoute(name, points, closeLoop, loopCount)
                    if (saved != null) {
                        editingRoute = saved
                        screen = AppDestination.HOME
                    }
                }
            )
        }
    }
}
