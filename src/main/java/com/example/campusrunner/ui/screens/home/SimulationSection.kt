package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.data.SavedPoint
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.data.SpeedPreset
import com.example.campusrunner.ui.RuntimeSession
import com.example.campusrunner.ui.RuntimeSessionKind
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeTextField
import com.example.campusrunner.ui.formatCoordinate
import com.example.campusrunner.ui.formatDistance
import com.example.campusrunner.ui.formatNumber
import com.example.campusrunner.ui.routeDisplayName
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeIconSizes
import com.example.campusrunner.ui.map.CampusMapView
import com.example.campusrunner.ui.map.MapController
import com.example.campusrunner.ui.map.MarkerKind
import com.example.campusrunner.ui.map.renderRoute
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import com.example.campusrunner.ui.theme.RouteVergeMotion
import com.example.campusrunner.ui.theme.rememberRouteVergeReducedMotion

internal enum class SimulationMode { POINT, ROUTE }

/**
 * Core simulation workflow: mode selector (point / route), contextual
 * configuration, one primary action, and — while running — low-noise
 * playback controls instead of configuration.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    onStop: () -> Unit
) {
    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()
    val selectedPoint = savedPoints.firstOrNull { it.id == selectedPointId }

    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(selected = mode == SimulationMode.POINT, onClick = { onModeChange(SimulationMode.POINT) }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("定点") }
            SegmentedButton(selected = mode == SimulationMode.ROUTE, onClick = { onModeChange(SimulationMode.ROUTE) }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("路线") }
        }
        val reducedMotion = rememberRouteVergeReducedMotion()
        // Keep the embedded map outside AnimatedContent: switching the mode
        // must not create two AndroidView instances or wait for map work.
        MapPreview(provider = mapProvider, mode = mode, route = selectedRoute, point = selectedPoint, lat = pointLatInput, lng = pointLngInput)
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else fadeIn(RouteVergeMotion.tweenSpec(RouteVergeMotion.contentDuration)) togetherWith
                    fadeOut(RouteVergeMotion.tweenSpec(RouteVergeMotion.contentDuration))
            },
            label = "simulation_mode_content"
        ) { modeState ->
            AnimatedContent(
                    targetState = isServiceRunning || isServicePaused,
                    transitionSpec = {
                        if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (fadeIn(RouteVergeMotion.tweenSpec()) + scaleIn(initialScale = 0.98f)) togetherWith
                            (fadeOut(RouteVergeMotion.tweenSpec()) + scaleOut(targetScale = 0.98f))
                    },
                    label = "simulation_controls_content"
                ) { running ->
                    if (running) {
                        RuntimeSurface(session = runtimeSession, isServicePaused = isServicePaused, onPause = onPause, onResumeMock = onResumeMock, onStop = onStop)
                    } else {
                        when (modeState) {
                            SimulationMode.POINT -> PointConfiguration(pointLatInput, pointLngInput, onPointLatChange, onPointLngChange, onOpenPointPicker, onStartPoint)
                            SimulationMode.ROUTE -> if (routes.isEmpty()) RouteEmptyState(onCreateRoute = onOpenRouteEditor) else RouteConfiguration(selectedRoute, speedText, onSpeedTextChange, onOpenRouteEditor) { selectedRoute?.let { route -> onStartRoute(route, speedText) } }
                        }
                    }
                }
        }
    }
}

@Composable
private fun MapPreview(
    provider: MapProvider,
    mode: SimulationMode,
    route: SavedRoute?,
    point: SavedPoint?,
    lat: String,
    lng: String
) {
    var controller by remember { mutableStateOf<MapController?>(null) }
    var markerHandle by remember { mutableStateOf<com.example.campusrunner.ui.map.MapMarkerHandle?>(null) }
    var markerVisible by remember { mutableStateOf(false) }
    var lastCameraKey by remember { mutableStateOf<String?>(null) }
    val reducedMotion = rememberRouteVergeReducedMotion()
    val markerAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (markerVisible) 1f else 0f,
        animationSpec = RouteVergeMotion.spec(reducedMotion, RouteVergeMotion.contentDuration),
        label = "map_marker_alpha"
    )
    LaunchedEffect(markerHandle, markerAlpha) { markerHandle?.setAlpha(markerAlpha) }
    val fallback = com.example.campusrunner.data.RoutePoint(lat.toDoubleOrNull() ?: 39.9042, lng.toDoubleOrNull() ?: 116.4074)
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().height(220.dp).clip(RouteVergeShapes.extraLarge)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RouteVergeShapes.extraLarge)
    ) {
        CampusMapView(provider, Modifier.fillMaxWidth().height(220.dp)) { c -> controller = c; c.disableUiControls() }
        LaunchedEffect(controller, mode, route?.id, point?.id, lat, lng) {
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
private fun PointConfiguration(
    latInput: String,
    lngInput: String,
    onLatChange: (String) -> Unit,
    onLngChange: (String) -> Unit,
    onOpenPointPicker: () -> Unit,
    onStartPoint: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
            RouteVergeTextField(
                value = latInput,
                onValueChange = onLatChange,
                label = "纬度 WGS-84",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            RouteVergeTextField(
                value = lngInput,
                onValueChange = onLngChange,
                label = "经度 WGS-84",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
            RouteVergeButton(
                onClick = onOpenPointPicker,
                variant = RouteVergeButtonVariant.Outlined,
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) },
                text = { Text("地图选点") }
            )
            RouteVergeButton(
                onClick = onStartPoint,
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) },
                text = { Text("开始定点") }
            )
        }
    }
}

@Composable
private fun RouteConfiguration(
    selectedRoute: SavedRoute?,
    speedText: String,
    onSpeedTextChange: (String) -> Unit,
    onOpenRouteEditor: () -> Unit,
    onStartRoute: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        SpeedSelector(speedText = speedText, onSpeedTextChange = onSpeedTextChange)
        Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            RouteVergeButton(onClick = onOpenRouteEditor, variant = RouteVergeButtonVariant.Outlined, modifier = Modifier.weight(1f), text = { Text("新建路线") })
            RouteVergeButton(onClick = onStartRoute, enabled = selectedRoute != null, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("开始路线") })
        }
    }
}

@Composable
private fun SpeedSelector(
    speedText: String,
    onSpeedTextChange: (String) -> Unit
) {
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
                    .height(64.dp)
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
                .height(64.dp)
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
        color = backgroundColor,
        contentColor = contentColor,
        shape = RouteVergeShapes.medium,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent),
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
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
    val pauseTransition = updateTransition(isServicePaused, label = "pause_state_transition")
    val pauseContentScale by pauseTransition.animateFloat(
        transitionSpec = { RouteVergeMotion.spec(reducedMotion, RouteVergeMotion.contentDuration) },
        label = "pause_content_scale"
    ) { paused -> if (paused) 0.98f else 1f }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = com.example.campusrunner.ui.theme.RouteVergeShapes.medium,
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
                RouteVergeButton(onClick = onStop, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("停止模拟") })
            } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    RouteVergeButton(onClick = if (isServicePaused) onResumeMock else onPause, modifier = Modifier.weight(1f).graphicsLayer { scaleX = pauseContentScale; scaleY = pauseContentScale }, leadingIcon = {
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
                    RouteVergeButton(onClick = onStop, modifier = Modifier.weight(1f), leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) }, text = { Text("停止模拟") })
                }
            }
        }
    }
}
