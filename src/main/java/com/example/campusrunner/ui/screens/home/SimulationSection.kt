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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import com.example.campusrunner.data.SpeedPreset
import com.example.campusrunner.ui.RuntimeSession
import com.example.campusrunner.ui.RuntimeSessionKind
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeCard
import com.example.campusrunner.ui.components.RouteVergeTextField
import com.example.campusrunner.ui.formatCoordinate
import com.example.campusrunner.ui.formatDistance
import com.example.campusrunner.ui.formatNumber
import com.example.campusrunner.ui.routeDisplayName
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing

private enum class SimulationMode { POINT, ROUTE }

/**
 * Core simulation workflow: mode selector (point / route), contextual
 * configuration, one primary action, and — while running — low-noise
 * playback controls instead of configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationSection(
    routes: List<SavedRoute>,
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
    var mode by remember { mutableStateOf(SimulationMode.POINT) }
    var routeMenuExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf(routes.firstOrNull()?.id) }
    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        if (isServiceRunning || isServicePaused) {
            RuntimeSurface(
                session = runtimeSession,
                isServicePaused = isServicePaused,
                onPause = onPause,
                onResumeMock = onResumeMock,
                onStop = onStop
            )
        } else {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == SimulationMode.POINT,
                    onClick = { mode = SimulationMode.POINT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("定点")
                }
                SegmentedButton(
                    selected = mode == SimulationMode.ROUTE,
                    onClick = { mode = SimulationMode.ROUTE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("路线")
                }
            }

            when (mode) {
                SimulationMode.POINT -> PointConfiguration(
                    latInput = pointLatInput,
                    lngInput = pointLngInput,
                    onLatChange = onPointLatChange,
                    onLngChange = onPointLngChange,
                    onOpenPointPicker = onOpenPointPicker,
                    onStartPoint = onStartPoint
                )

                SimulationMode.ROUTE -> if (routes.isEmpty()) {
                    // No saved route yet: the next real step is creating one,
                    // not configuring speed for a route that does not exist.
                    RouteEmptyState(onCreateRoute = onOpenRouteEditor)
                } else {
                    RouteConfiguration(
                        routes = routes,
                        selectedRoute = selectedRoute,
                        onSelectRoute = { selectedRouteId = it },
                        routeMenuExpanded = routeMenuExpanded,
                        onRouteMenuExpandedChange = { routeMenuExpanded = it },
                        speedText = speedText,
                        onSpeedTextChange = onSpeedTextChange,
                        onOpenRouteEditor = onOpenRouteEditor,
                        onStartRoute = {
                            selectedRoute?.let { route -> onStartRoute(route, speedText) }
                        }
                    )
                }
            }
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
    onStartPoint: () -> Unit
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
                leadingIcon = { Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("地图选点") }
            )
            RouteVergeButton(
                onClick = onStartPoint,
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("开始定点") }
            )
        }
    }
}

@Composable
private fun RouteConfiguration(
    routes: List<SavedRoute>,
    selectedRoute: SavedRoute?,
    onSelectRoute: (String) -> Unit,
    routeMenuExpanded: Boolean,
    onRouteMenuExpandedChange: (Boolean) -> Unit,
    speedText: String,
    onSpeedTextChange: (String) -> Unit,
    onOpenRouteEditor: () -> Unit,
    onStartRoute: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
        Box {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RouteVergeShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = routes.isNotEmpty()) { onRouteMenuExpandedChange(true) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = RouteVergeSpacing.lg, vertical = RouteVergeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(RouteVergeSpacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedRoute?.name ?: if (routes.isEmpty()) "还没有保存路线" else "选择路线",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        selectedRoute?.let { route ->
                            val distance = RouteMath.totalDistanceMeters(route.points, closeLoop = route.closeLoop)
                            Text(
                                "${formatDistance(distance)} · ${route.points.size} 个点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = routeMenuExpanded,
                onDismissRequest = { onRouteMenuExpandedChange(false) }
            ) {
                routes.forEach { route ->
                    DropdownMenuItem(
                        text = { Text(route.name) },
                        onClick = {
                            onSelectRoute(route.id)
                            onRouteMenuExpandedChange(false)
                        }
                    )
                }
            }
        }

        SpeedSelector(speedText = speedText, onSpeedTextChange = onSpeedTextChange)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onOpenRouteEditor) {
                Text("新建路线")
            }
        }

        RouteVergeButton(
            onClick = onStartRoute,
            enabled = selectedRoute != null,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) },
            text = { Text("开始路线") }
        )
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
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
    RouteVergeCard {
        Column(
            modifier = Modifier.padding(RouteVergeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)
        ) {
            Text(
                if (isServicePaused) "模拟暂停" else "模拟运行中",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

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

            Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                RouteVergeButton(
                    onClick = if (isServicePaused) onResumeMock else onPause,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            if (isServicePaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = { Text(if (isServicePaused) "继续" else "暂停") }
                )
                RouteVergeButton(
                    onClick = onStop,
                    variant = RouteVergeButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("停止") }
                )
            }
        }
    }
}
