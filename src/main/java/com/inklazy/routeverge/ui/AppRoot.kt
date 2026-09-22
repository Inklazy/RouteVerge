package com.inklazy.routeverge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import com.inklazy.routeverge.data.MapProvider
import com.inklazy.routeverge.data.RoutePoint
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.data.SavedPoint
import com.inklazy.routeverge.data.SpeedPreset
import com.inklazy.routeverge.ui.components.RouteVergeStatus
import com.inklazy.routeverge.ui.navigation.AppDestination
import com.inklazy.routeverge.ui.screens.home.HomeScreen
import com.inklazy.routeverge.ui.screens.route.MapPointPickerScreen
import com.inklazy.routeverge.ui.screens.settings.SettingsScreen
import com.inklazy.routeverge.ui.screens.route.RouteEditorScreen
import java.util.Locale
import com.inklazy.routeverge.ui.theme.RouteVergeMotion
import com.inklazy.routeverge.ui.theme.rememberRouteVergeReducedMotion

/**
 * App composition root: owns navigation state (which destination is shown),
 * the back handling and the light cross-screen UI state (route editor draft,
 * point picker draft, speed). It receives all app state + actions from the
 * Activity — no ViewModel, state source is unchanged.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppRoot(
    uiState: RouteVergeUiState,
    isNfcActivated: Boolean,
    nfcLinkConfigured: Boolean,
    onMapProviderChange: (MapProvider) -> Unit,
    onShowCurrentLinkDialog: () -> Unit,
    onOpenProjectHome: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onVerifyNfc: () -> Unit,
    onOpenAlipayNfc: () -> Unit,
    onStartPoint: (String, String) -> Unit,
    onStartRoute: (SavedRoute, String) -> Unit,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit,
    onSavePoint: (String?, String, String) -> Boolean,
    onUpdatePoint: (String, String?, String, String) -> Boolean,
    onDeletePoint: (String) -> Unit,
    onSaveRoute: (String, List<RoutePoint>, Boolean, Int) -> SavedRoute?,
    onLocateMe: () -> RoutePoint?
) {
    // This is deliberately a real (small) back stack rather than a single
    // destination flag. A route editor therefore returns to the exact Home
    // entry it was opened from instead of relying on Home's default mode.
    // Save the stack as a delimiter-separated path so the state registry only
    // needs to persist a String (and never a mutable collection).
    var backStackPath by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    val backStack = backStackPath.split('|')
    val screen = AppDestination.valueOf(backStack.last())
    fun navigateTo(destination: AppDestination) {
        backStackPath = "$backStackPath|${destination.name}"
    }
    fun popBackStack() {
        if (backStack.size > 1) backStackPath = backStack.dropLast(1).joinToString("|")
    }
    fun popToHome() {
        if (backStack.size > 1) backStackPath = AppDestination.HOME.name
    }

    // Home stays the owner of its presentation state while a child screen is
    // open. Keeping these above AnimatedContent preserves selection, preview
    // inputs and both independent saved-record scroll positions on return.
    var homeModeName by rememberSaveable { mutableStateOf(com.inklazy.routeverge.ui.screens.home.SimulationMode.POINT.name) }
    val homeMode = com.inklazy.routeverge.ui.screens.home.SimulationMode.valueOf(homeModeName)
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPointId by rememberSaveable { mutableStateOf<String?>(null) }
    val pointRecordsState = rememberLazyListState()
    val routeRecordsState = rememberLazyListState()
    var editingRoute by remember { mutableStateOf<SavedRoute?>(null) }
    var speedText by remember { mutableStateOf(formatNumber(SpeedPreset.FAST_PACE.speedMps)) }
    var closeLoop by remember { mutableStateOf(false) }
    var loopCountText by remember { mutableStateOf("1") }
    var pointLatInput by remember { mutableStateOf("39.904200") }
    var pointLngInput by remember { mutableStateOf("116.407400") }
    var pendingPoint by remember { mutableStateOf<RoutePoint?>(null) }
    var pendingPointName by remember { mutableStateOf("") }
    var pendingPointEditId by remember { mutableStateOf<String?>(null) }
    var savingPoint by remember { mutableStateOf(false) }

    BackHandler(enabled = backStack.size > 1) {
        popBackStack()
    }

    val reducedMotion = rememberRouteVergeReducedMotion()
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (reducedMotion) fadeIn(androidx.compose.animation.core.tween(0)) togetherWith fadeOut(androidx.compose.animation.core.tween(0))
            else fadeIn(RouteVergeMotion.tweenSpec(RouteVergeMotion.contentDuration)) togetherWith fadeOut(RouteVergeMotion.tweenSpec(RouteVergeMotion.contentDuration))
        },
        label = "screen_transition"
    ) { target ->
        when (target) {
            AppDestination.HOME -> HomeScreen(
                hasLocationPermission = uiState.hasLocationPermission,
                canMockLocation = uiState.canMockLocation,
                routes = uiState.routes,
                savedPoints = uiState.savedPoints,
                mapProvider = uiState.mapProvider,
                selectedRouteId = selectedRouteId,
                selectedPointId = selectedPointId,
                mode = homeMode,
                pointRecordsState = pointRecordsState,
                routeRecordsState = routeRecordsState,
                isServiceRunning = uiState.isServiceRunning,
                isServicePaused = uiState.isServicePaused,
                runtimeSession = uiState.runtimeSession,
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
                onModeChange = { homeModeName = it.name },
                onSelectedRouteChange = { selectedRouteId = it },
                onSelectedPointChange = { selectedPointId = it },
                onOpenSettings = { navigateTo(AppDestination.SETTINGS) },
                onOpenPointPicker = { pendingPointEditId = null; pendingPointName = ""; navigateTo(AppDestination.POINT_PICKER) },
                onEditPoint = { point ->
                    pendingPointEditId = point.id
                    pendingPointName = point.name
                    pointLatInput = String.format(Locale.US, "%.6f", point.point.latWgs84)
                    pointLngInput = String.format(Locale.US, "%.6f", point.point.lngWgs84)
                    navigateTo(AppDestination.POINT_PICKER)
                },
                onDeletePoint = onDeletePoint,
                onOpenRouteEditor = {
                    editingRoute = null
                    closeLoop = false
                    loopCountText = "1"
                    navigateTo(AppDestination.ROUTE_EDITOR)
                },
                onEditRoute = {
                    editingRoute = it
                    closeLoop = it.closeLoop
                    loopCountText = it.loopCount.toString()
                    navigateTo(AppDestination.ROUTE_EDITOR)
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
                onBack = ::popBackStack,
                onVerifyNfc = onVerifyNfc,
                onShowCurrentLink = onShowCurrentLinkDialog,
                onOpenProjectHome = onOpenProjectHome
            )

            AppDestination.POINT_PICKER -> MapPointPickerScreen(
                mapProvider = uiState.mapProvider,
                onMapProviderChange = onMapProviderChange,
                runtimeEntryText = runtimeSummary(uiState.isServiceRunning, uiState.isServicePaused, uiState.runtimeSession),
                runtimeEntryStatus = when {
                    uiState.isServicePaused -> RouteVergeStatus.Paused
                    uiState.isServiceRunning -> RouteVergeStatus.Running
                    else -> null
                },
                onRuntimeEntryClick = ::popToHome,
                startPoint = pointLatInput.toDoubleOrNull()?.let { lat ->
                    pointLngInput.toDoubleOrNull()?.let { lng -> RoutePoint(lat, lng) }
                },
                onBack = ::popBackStack,
                onLocateMe = onLocateMe,
                onPointPicked = { point ->
                    pointLatInput = String.format(Locale.US, "%.6f", point.latWgs84)
                    pointLngInput = String.format(Locale.US, "%.6f", point.lngWgs84)
                    pendingPoint = point
                    if (pendingPointEditId == null) pendingPointName = ""
                }
            )

            AppDestination.ROUTE_EDITOR -> RouteEditorScreen(
                mapProvider = uiState.mapProvider,
                onMapProviderChange = onMapProviderChange,
                runtimeEntryText = runtimeSummary(uiState.isServiceRunning, uiState.isServicePaused, uiState.runtimeSession),
                runtimeEntryStatus = when {
                    uiState.isServicePaused -> RouteVergeStatus.Paused
                    uiState.isServiceRunning -> RouteVergeStatus.Running
                    else -> null
                },
                onRuntimeEntryClick = ::popToHome,
                initialRoute = editingRoute,
                closeLoop = closeLoop,
                loopCountText = loopCountText,
                onCloseLoopChange = { closeLoop = it },
                onLoopCountTextChange = { loopCountText = it },
                onBack = ::popBackStack,
                onLocateMe = onLocateMe,
                onSaveRoute = { name, points ->
                    val loopCount = loopCountText.toIntOrNull() ?: 1
                    val saved = onSaveRoute(name, points, closeLoop, loopCount)
                    if (saved != null) {
                        editingRoute = saved
                        selectedRouteId = saved.id
                        popToHome()
                    }
                }
            )
        }
    }

    pendingPoint?.let { _ ->
        AlertDialog(
            onDismissRequest = { if (!savingPoint) { pendingPoint = null; pendingPointEditId = null } },
            title = { Text(if (pendingPointEditId == null) "保存点位" else "更新点位") },
            text = { OutlinedTextField(value = pendingPointName, onValueChange = { pendingPointName = it }, label = { Text("名称（可选）") }, singleLine = true) },
            confirmButton = {
                Button(enabled = !savingPoint, onClick = {
                    if (!savingPoint) {
                    savingPoint = true
                    val saved = pendingPointEditId?.let { id ->
                        onUpdatePoint(id, pendingPointName, pointLatInput, pointLngInput)
                    } ?: onSavePoint(pendingPointName, pointLatInput, pointLngInput)
                    savingPoint = false
                    if (saved) {
                        pendingPoint = null
                        pendingPointEditId = null
                        popToHome()
                    }
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(enabled = !savingPoint, onClick = { pendingPoint = null; pendingPointEditId = null }) { Text("取消") } }
        )
    }
}
