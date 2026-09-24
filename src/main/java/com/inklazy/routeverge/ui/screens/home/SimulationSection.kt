package com.inklazy.routeverge.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.data.SavedPoint
import com.inklazy.routeverge.data.RoutePoint
import com.inklazy.routeverge.data.MapProvider
import com.inklazy.routeverge.data.PlaybackMode
import com.inklazy.routeverge.data.SpeedPreset
import com.inklazy.routeverge.ui.RuntimeSession
import com.inklazy.routeverge.ui.RuntimeSessionKind
import com.inklazy.routeverge.geo.RouteMath
import com.inklazy.routeverge.ui.components.RouteVergeButton
import com.inklazy.routeverge.ui.components.RouteVergeButtonVariant
import com.inklazy.routeverge.ui.components.RouteVergeButtonHapticFeedback
import com.inklazy.routeverge.ui.components.RouteVergePressFeedback
import com.inklazy.routeverge.ui.components.rememberModeSwitchHaptic
import com.inklazy.routeverge.ui.components.RouteVergeTextField
import com.inklazy.routeverge.ui.formatCoordinate
import com.inklazy.routeverge.ui.formatDistance
import com.inklazy.routeverge.ui.formatNumber
import com.inklazy.routeverge.ui.routeDisplayName
import com.inklazy.routeverge.ui.theme.RouteVergeShapes
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeIconSizes
import com.inklazy.routeverge.ui.map.CampusMapView
import com.inklazy.routeverge.ui.map.MapController
import com.inklazy.routeverge.ui.map.MarkerKind
import com.inklazy.routeverge.ui.map.renderRoute
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import com.inklazy.routeverge.ui.theme.RouteVergeMotion
import com.inklazy.routeverge.ui.theme.rememberRouteVergeReducedMotion
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

internal enum class SimulationMode { POINT, ROUTE }

/**
 * Core simulation workflow: mode selector (point / route), contextual
 * configuration, one primary action, and — while running — low-noise
 * playback controls instead of configuration.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun SimulationSection(
    routes: List<SavedRoute>,
    savedPoints: List<SavedPoint>,
    mapProvider: MapProvider,
    selectedRouteId: String?,
    mode: SimulationMode,
    onModeChange: (SimulationMode) -> Unit,
    selectedPointId: String?,
    runtimeSession: RuntimeSession?,
    speedText: String,
    pointLatInput: String,
    pointLngInput: String,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    onPointLatChange: (String) -> Unit,
    onPointLngChange: (String) -> Unit,
    onSpeedTextChange: (String) -> Unit,
    onOpenPointPicker: () -> Unit,
    onOpenRouteEditor: () -> Unit,
    onStartPoint: () -> Unit,
    onStartRoute: (SavedRoute, String) -> Unit,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit,
    mapHeight: androidx.compose.ui.unit.Dp
) {
    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()
    val selectedPoint = savedPoints.firstOrNull { it.id == selectedPointId }

    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        ModeSelector(mode = mode, onModeChange = onModeChange)
        val reducedMotion = rememberRouteVergeReducedMotion()
        // Keep the embedded map outside AnimatedContent: switching the mode
        // must not create two AndroidView instances or wait for map work.
        MapPreview(provider = mapProvider, mode = mode, route = selectedRoute, point = selectedPoint, lat = pointLatInput, lng = pointLngInput, runtimeSession = runtimeSession, height = mapHeight)
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else fadeIn(RouteVergeMotion.tweenSpec(RouteVergeMotion.contentDuration)) togetherWith
                    fadeOut(RouteVergeMotion.tweenSpec(RouteVergeMotion.contentDuration))
            },
            label = "simulation_mode_content"
        ) { modeState ->
            // The map is not a child of this transition. These are only
            // lightweight control changes and therefore remain responsive even
            // while the map SDK renders route geometry.
            val active = isServiceRunning || isServicePaused
            val sessionModeDiffers = active && (runtimeSession == null ||
                (modeState == SimulationMode.POINT && runtimeSession.kind == RuntimeSessionKind.ROUTE) ||
                    (modeState == SimulationMode.ROUTE && runtimeSession.kind == RuntimeSessionKind.POINT))
            if (sessionModeDiffers) {
                // A user may switch the selector while a session is active;
                // keep showing controls for the real session, never a second
                // start action for the newly selected mode.
                RuntimeSurface(session = runtimeSession, isServicePaused = isServicePaused, onPause = onPause, onResumeMock = onResumeMock, onStop = onStop)
            } else when (modeState) {
                SimulationMode.POINT -> PointConfiguration(
                    pointLatInput, pointLngInput, onPointLatChange, onPointLngChange,
                    onOpenPointPicker, onStartPoint, runtimeSession, isServiceRunning,
                    isServicePaused, onStop
                )
                SimulationMode.ROUTE -> if (routes.isEmpty()) RouteEmptyState(onCreateRoute = onOpenRouteEditor)
                else RouteConfiguration(
                    selectedRoute, speedText, onSpeedTextChange, onOpenRouteEditor,
                    { selectedRoute?.let { route -> onStartRoute(route, speedText) } },
                    runtimeSession, isServiceRunning, isServicePaused, onPause,
                    onResumeMock, onStop
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    mode: SimulationMode,
    onModeChange: (SimulationMode) -> Unit
) {
    val reducedMotion = rememberRouteVergeReducedMotion()
    val modeSwitchHaptic = rememberModeSwitchHaptic()
    val modeScope = rememberCoroutineScope()
    // Animate raw dp values so the completion callback is available from
    // Animatable.animateTo; convert back to Dp only for layout.
    val capsuleOffset = remember { Animatable(0f) }
    var modeAnimationJob by remember { mutableStateOf<Job?>(null) }
    var modeAnimationGeneration by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                modeAnimationGeneration += 1
                modeAnimationJob?.cancel()
                modeAnimationJob = null
                modeSwitchHaptic.cancel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            modeAnimationGeneration += 1
            modeAnimationJob?.cancel()
            modeSwitchHaptic.cancel()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RouteVergeShapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RouteVergeShapes.extraLarge)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(2.dp)
        ) {
            // maxWidth/maxHeight are the actual inner area after the one,
            // symmetric 2dp inset. Both positions use this same measured
            // width, so neither direction accumulates a rounding discrepancy.
            val indicatorWidth = maxWidth / 2
            val targetOffset = if (mode == SimulationMode.POINT) 0.dp else indicatorWidth
            LaunchedEffect(indicatorWidth) {
                modeAnimationJob?.cancel()
                capsuleOffset.snapTo(targetOffset.value)
                modeAnimationJob = null
            }
            val indicatorOffset = capsuleOffset.value.dp
            fun selectMode(targetMode: SimulationMode) {
                if (targetMode == mode) return
                modeAnimationJob?.cancel()
                val generation = modeAnimationGeneration + 1
                modeAnimationGeneration = generation
                modeSwitchHaptic.perform(RouteVergeMotion.modeSelectorDuration)
                val target = (if (targetMode == SimulationMode.POINT) 0.dp else indicatorWidth).value
                onModeChange(targetMode)
                modeAnimationJob = modeScope.launch {
                    capsuleOffset.animateTo(
                        target,
                        animationSpec = RouteVergeMotion.spec(reducedMotion, RouteVergeMotion.modeSelectorDuration)
                    )
                    if (modeAnimationGeneration == generation) {
                        modeSwitchHaptic.confirm()
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .clip(RouteVergeShapes.large)
                    // Selection is communicated by this single moving color
                    // block only; no child elevation/shadow can form a seam.
                    .background(Color(0xFFD8CDBB))
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                ModeOption("定点", mode == SimulationMode.POINT, { selectMode(SimulationMode.POINT) }, Modifier.weight(1f))
                ModeOption("路线", mode == SimulationMode.ROUTE, { selectMode(SimulationMode.ROUTE) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberRouteVergeReducedMotion()
    // The moving indicator is the selector's base layer. The custom pressed
    // overlay below is clipped to the same half-capsule so it never becomes a
    // smaller nested rectangle or a full-width rectangular ripple.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val color by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = RouteVergeMotion.spec(reducedMotion),
        label = "mode_selector_text_color"
    )
    val checkScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else .75f,
        animationSpec = RouteVergeMotion.spec(reducedMotion, 180),
        label = "mode_selector_check_scale"
    )
    val checkAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = RouteVergeMotion.spec(reducedMotion, 180),
        label = "mode_selector_check_alpha"
    )
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RouteVergeShapes.large)
            .background(
                RouteVergePressFeedback.color(
                    base = Color.Transparent,
                    content = MaterialTheme.colorScheme.onSurface,
                    pressed = pressed
                )
            )
            .semantics { stateDescription = if (selected) "$label，已选中" else "$label，未选中" }
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(RouteVergeIconSizes.small).graphicsLayer {
                scaleX = checkScale; scaleY = checkScale; alpha = checkAlpha
            }
        )
        Spacer(Modifier.width(RouteVergeSpacing.xs))
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun MapPreview(
    provider: MapProvider,
    mode: SimulationMode,
    route: SavedRoute?,
    point: SavedPoint?,
    lat: String,
    lng: String,
    runtimeSession: RuntimeSession?,
    height: androidx.compose.ui.unit.Dp
) {
    var controller by remember { mutableStateOf<MapController?>(null) }
    var markerHandle by remember { mutableStateOf<com.inklazy.routeverge.ui.map.MapMarkerHandle?>(null) }
    var markerVisible by remember { mutableStateOf(false) }
    var lastCameraKey by remember { mutableStateOf<String?>(null) }
    val reducedMotion = rememberRouteVergeReducedMotion()
    val markerAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (markerVisible) 1f else 0f,
        animationSpec = RouteVergeMotion.spec(reducedMotion, RouteVergeMotion.contentDuration),
        label = "map_marker_alpha"
    )
    LaunchedEffect(markerHandle, markerAlpha) { markerHandle?.setAlpha(markerAlpha) }
    val fallback = com.inklazy.routeverge.data.RoutePoint(lat.toDoubleOrNull() ?: 39.9042, lng.toDoubleOrNull() ?: 116.4074)
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().height(height).clip(RouteVergeShapes.extraLarge)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RouteVergeShapes.extraLarge)
    ) {
        CampusMapView(provider, Modifier.fillMaxWidth().height(height)) { c -> controller = c; c.disableUiControls() }
        val routeProgressKey = runtimeSession?.takeIf { it.kind == RuntimeSessionKind.ROUTE }?.activeElapsedMillis
        LaunchedEffect(controller, mode, route?.id, point?.id, lat, lng, routeProgressKey) {
            val c = controller ?: return@LaunchedEffect
            // Let the selector commit its frame first; SDK drawing work is
            // intentionally deferred so it cannot hold up the check icon.
            kotlinx.coroutines.yield()
            // Coordinate edits still refresh the marker, but do not replay a
            // zoom animation on every keystroke or mode recomposition.
            val cameraKey = if (mode == SimulationMode.ROUTE) "route:${route?.id}" else "point:${point?.id ?: "manual"}"
            if (mode == SimulationMode.POINT && point == null && markerHandle != null) {
                markerHandle?.setPosition(fallback)
                markerVisible = true
                return@LaunchedEffect
            }
            c.clear()
            markerHandle = null
            markerVisible = false
            if (mode == SimulationMode.ROUTE && route != null) {
                renderRoute(c, route.points, route.closeLoop)
                val activeRouteSession = runtimeSession?.takeIf { it.kind == RuntimeSessionKind.ROUTE }
                val speed = activeRouteSession?.speedMps
                if (speed != null && speed > 0.0) {
                    val playbackMode = if (activeRouteSession.closeLoop) PlaybackMode.LOOP else PlaybackMode.OUT_AND_BACK
                    val current = RouteMath.interpolateRoute(
                        points = route.points,
                        elapsedMillis = activeRouteSession.activeElapsedMillis,
                        speedMps = speed,
                        playbackMode = playbackMode
                    )
                    c.addMarker(
                        RoutePoint(current.latWgs84, current.lngWgs84),
                        "当前位置",
                        MarkerKind.CURRENT
                    )
                }
                if (cameraKey != lastCameraKey) route.points.firstOrNull()?.let { c.animateCamera(it, 15f) }
            } else if (mode == SimulationMode.POINT) {
                val p = fallback
                markerHandle = c.addMarker(p, point?.name ?: "当前点位", MarkerKind.CURRENT)
                if (cameraKey != lastCameraKey) c.animateCamera(p, 16f)
            }
            lastCameraKey = cameraKey
            markerVisible = true
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PointConfiguration(
    latInput: String,
    lngInput: String,
    onLatChange: (String) -> Unit,
    onLngChange: (String) -> Unit,
    onOpenPointPicker: () -> Unit,
    onStartPoint: () -> Unit,
    runtimeSession: RuntimeSession?,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    onStop: () -> Unit,
) {
    val reducedMotion = rememberRouteVergeReducedMotion()
    val bringIntoViewScope = rememberCoroutineScope()
    val latRequester = remember { BringIntoViewRequester() }
    val lngRequester = remember { BringIntoViewRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
            RouteVergeTextField(
                value = latInput,
                onValueChange = onLatChange,
                label = "纬度 WGS-84",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .bringIntoViewRequester(latRequester)
                    .onFocusChanged { if (it.isFocused) bringIntoViewScope.launch { latRequester.bringIntoView() } },
                singleLine = true
            )
            RouteVergeTextField(
                value = lngInput,
                onValueChange = onLngChange,
                label = "经度 WGS-84",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .bringIntoViewRequester(lngRequester)
                    .onFocusChanged { if (it.isFocused) bringIntoViewScope.launch { lngRequester.bringIntoView() } },
                singleLine = true
            )
        }
        AnimatedContent(
            targetState = isServiceRunning || isServicePaused,
            transitionSpec = { if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0)) else fadeIn(RouteVergeMotion.tweenSpec(220)) togetherWith fadeOut(RouteVergeMotion.tweenSpec(220)) },
            label = "point_action_transition"
        ) { running ->
            if (running && runtimeSession?.kind != RuntimeSessionKind.ROUTE) {
                RouteVergeButton(onClick = onStop, hapticFeedback = RouteVergeButtonHapticFeedback.HighPriority, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("停止模拟") })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    RouteVergeButton(onClick = onOpenPointPicker, variant = RouteVergeButtonVariant.Outlined, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("地图选点") })
                    RouteVergeButton(onClick = onStartPoint, hapticFeedback = RouteVergeButtonHapticFeedback.HighPriority, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("开始定点") })
                }
            }
        }
    }
}

@Composable
private fun RouteConfiguration(
    selectedRoute: SavedRoute?,
    speedText: String,
    onSpeedTextChange: (String) -> Unit,
    onOpenRouteEditor: () -> Unit,
    onStartRoute: () -> Unit,
    runtimeSession: RuntimeSession?,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit
) {
    val reducedMotion = rememberRouteVergeReducedMotion()
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        SpeedSelector(speedText = speedText, onSpeedTextChange = onSpeedTextChange)
        AnimatedContent(
            targetState = isServiceRunning || isServicePaused,
            transitionSpec = { if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0)) else fadeIn(RouteVergeMotion.tweenSpec(220)) togetherWith fadeOut(RouteVergeMotion.tweenSpec(220)) },
            label = "route_action_transition"
        ) { running ->
            if (running && runtimeSession?.kind != RuntimeSessionKind.POINT) {
                Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    RouteVergeButton(onClick = if (isServicePaused) onResumeMock else onPause, variant = RouteVergeButtonVariant.Outlined, modifier = Modifier.weight(1f), leadingIcon = { Icon(if (isServicePaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text(if (isServicePaused) "继续模拟" else "暂停模拟") })
                    RouteVergeButton(onClick = onStop, hapticFeedback = RouteVergeButtonHapticFeedback.HighPriority, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("停止模拟") })
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    RouteVergeButton(onClick = onOpenRouteEditor, variant = RouteVergeButtonVariant.Outlined, modifier = Modifier.weight(1f), text = { Text("新建路线") })
                    RouteVergeButton(onClick = onStartRoute, enabled = selectedRoute != null, hapticFeedback = RouteVergeButtonHapticFeedback.HighPriority, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("开始路线") })
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SpeedSelector(
    speedText: String,
    onSpeedTextChange: (String) -> Unit
) {
    val bringIntoViewScope = rememberCoroutineScope()
    val speedRequester = remember { BringIntoViewRequester() }
    Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
        val currentSpeed = speedText.toDoubleOrNull()
        SpeedPreset.entries.forEach { preset ->
            SpeedPresetButton(
                label = preset.label,
                speedMps = preset.speedMps,
                selected = currentSpeed?.let { kotlin.math.abs(it - preset.speedMps) < 0.001 } == true,
                onClick = { onSpeedTextChange(formatNumber(preset.speedMps)) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            )
        }
        RouteVergeTextField(
            value = speedText,
            onValueChange = onSpeedTextChange,
            label = "m/s",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(0.82f)
                .height(56.dp)
                .bringIntoViewRequester(speedRequester)
                .onFocusChanged { if (it.isFocused) bringIntoViewScope.launch { speedRequester.bringIntoView() } }
        )
    }
}

@Composable
private fun SpeedPresetButton(
    label: String,
    speedMps: Double,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberRouteVergeReducedMotion()
    // The preset owns its interaction target. Disable the default selectable
    // indication because it is rectangular and can bleed past the card shape.
    // Selection and press feedback are both expressed by the same rounded
    // surface color, so there is only one visible state layer.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cardShape = RouteVergeShapes.medium
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = RouteVergeMotion.spec(reducedMotion),
        label = "speed_selection_color"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = RouteVergeMotion.spec(reducedMotion),
        label = "speed_selection_content"
    )
    Surface(
        color = RouteVergePressFeedback.color(
            base = backgroundColor,
            content = contentColor,
            pressed = pressed
        ),
        contentColor = contentColor,
        shape = cardShape,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.outline else Color.Transparent),
        modifier = modifier
            // Keep the surface fill and the press color inside the same
            // rounded bounds.
            .clip(cardShape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RouteVergeSpacing.xs, vertical = RouteVergeSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatNumber(speedMps)}m/s",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun RouteEmptyState(onCreateRoute: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RouteVergeSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
    ) {
        Text("还没有保存路线", style = MaterialTheme.typography.titleSmall)
        Text(
            "创建路线后即可开始路线模拟",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(RouteVergeSpacing.md))
        RouteVergeButton(
            onClick = onCreateRoute,
            variant = RouteVergeButtonVariant.Outlined
        ) {
            Text("创建路线")
        }
    }
}

/**
 * Runtime surface shown while a simulation is running or paused.
 * Status -> what is being simulated -> short config context -> primary control.
 * No fake telemetry: only the recorded session description is shown.
 */
@Composable
private fun RuntimeSurface(
    session: RuntimeSession?,
    isServicePaused: Boolean,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit
) {
    val reducedMotion = rememberRouteVergeReducedMotion()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = com.inklazy.routeverge.ui.theme.RouteVergeShapes.medium,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(RouteVergeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)
        ) {
            when {
                session == null -> {
                    // Activity recreation / unknown session: never guess context.
                    Text("当前会话", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "正在模拟位置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                session.kind == RuntimeSessionKind.POINT -> {
                    Text("定点模拟", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        formatCoordinate(session.pointLat ?: 0.0) + " · " + formatCoordinate(session.pointLng ?: 0.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        routeDisplayName(session.routeName),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val speed = session.speedMps?.let { formatNumber(it) + " m/s" }
                    val mode = if (session.closeLoop) "闭环 " + (session.loopCount.coerceAtLeast(1)) + " 圈" else "往返"
                    Text(
                        listOfNotNull(speed, mode).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    session.totalDistanceMeters?.let { distance ->
                        Text(
                            "路线全长 " + formatDistance(distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val pointMode = session?.kind == RuntimeSessionKind.POINT
            if (pointMode) {
                RouteVergeButton(onClick = onStop, hapticFeedback = RouteVergeButtonHapticFeedback.HighPriority, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("停止模拟") })
            } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    RouteVergeButton(onClick = if (isServicePaused) onResumeMock else onPause, variant = RouteVergeButtonVariant.Outlined, modifier = Modifier.weight(1f), leadingIcon = {
                        AnimatedContent(targetState = isServicePaused, transitionSpec = { if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0)) else (fadeIn(RouteVergeMotion.tweenSpec()) + scaleIn(initialScale = 0.96f)) togetherWith (fadeOut(RouteVergeMotion.tweenSpec()) + scaleOut(targetScale = 0.96f)) }, label = "pause_resume_icon") { paused ->
                            Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard))
                        }
                    }, text = {
                        AnimatedContent(
                            targetState = isServicePaused,
                            transitionSpec = { if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0)) else (fadeIn(RouteVergeMotion.tweenSpec()) + scaleIn(initialScale = 0.96f)) togetherWith (fadeOut(RouteVergeMotion.tweenSpec()) + scaleOut(targetScale = 0.96f)) },
                            label = "pause_resume_label"
                        ) { paused -> Text(if (paused) "继续模拟" else "暂停模拟") }
                    })
                    RouteVergeButton(onClick = onStop, hapticFeedback = RouteVergeButtonHapticFeedback.HighPriority, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("停止模拟") })
                }
            }
        }
    }
}
