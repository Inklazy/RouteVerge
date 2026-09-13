package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.data.SavedPoint
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.ui.RuntimeSession
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.components.PointRecord
import com.example.campusrunner.ui.components.SavedRecordRow
import com.example.campusrunner.ui.formatCoordinate
import com.example.campusrunner.ui.theme.RouteVergeSpacing

// Compose Foundation compatibility: animateItem is named animateItemPlacement
// in the resolved runtime version, while keeping one project-level call site.
@OptIn(ExperimentalFoundationApi::class)
private fun LazyItemScope.animateItem(): Modifier = with(this) { Modifier.animateItem() }

/**
 * Home destination — a single vertical flow:
 * status -> simulation (mode + config + primary action) -> saved routes -> tools.
 * No card stacks, no dashboard. Technical state lives in the status detail
 * (Phase 9 Settings), not on the first screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    hasLocationPermission: Boolean,
    canMockLocation: Boolean,
    routes: List<SavedRoute>,
    savedPoints: List<SavedPoint>,
    mapProvider: MapProvider,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    runtimeSession: RuntimeSession?,
    isNfcActivated: Boolean,
    speedText: String,
    pointLatInput: String,
    pointLngInput: String,
    onPointLatChange: (String) -> Unit,
    onPointLngChange: (String) -> Unit,
    onSpeedTextChange: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onVerifyNfc: () -> Unit,
    onOpenAlipayNfc: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPointPicker: () -> Unit,
    onOpenRouteEditor: () -> Unit,
    onEditRoute: (SavedRoute) -> Unit,
    onEditPoint: (SavedPoint) -> Unit,
    onDeletePoint: (String) -> Unit,
    onStartPoint: () -> Unit,
    onStartRoute: (SavedRoute, String) -> Unit,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit
) {
    var selectedRouteId by remember { mutableStateOf(routes.firstOrNull()?.id) }
    var selectedPointId by remember { mutableStateOf(savedPoints.firstOrNull()?.id) }
    var mode by remember { mutableStateOf(SimulationMode.POINT) }
    androidx.compose.runtime.LaunchedEffect(routes) {
        if (routes.none { it.id == selectedRouteId }) selectedRouteId = routes.firstOrNull()?.id
    }
    androidx.compose.runtime.LaunchedEffect(savedPoints) {
        if (savedPoints.none { it.id == selectedPointId }) selectedPointId = savedPoints.firstOrNull()?.id
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("RouteVerge") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    RouteVergeIconButton(
                        onClick = onOpenSettings,
                        variant = RouteVergeIconButtonVariant.Plain,
                        contentDescription = "设置"
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = RouteVergeSpacing.lg),
            contentPadding = PaddingValues(bottom = RouteVergeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.lg)
        ) {
            item {
                androidx.compose.foundation.layout.Box(animateItem()) {
                HomeStatusSection(
                    hasLocationPermission = hasLocationPermission,
                    canMockLocation = canMockLocation,
                    isServiceRunning = isServiceRunning,
                    isServicePaused = isServicePaused,
                    isNfcActivated = isNfcActivated,
                    onRequestPermissions = onRequestPermissions,
                    onOpenDeveloperOptions = onOpenDeveloperOptions,
                    onVerifyNfc = onVerifyNfc
                ) }
            }
            item {
                androidx.compose.foundation.layout.Box(animateItem()) {
                SimulationSection(
                    routes = routes,
                    savedPoints = savedPoints,
                    mapProvider = mapProvider,
                    selectedRouteId = selectedRouteId,
                    mode = mode,
                    onModeChange = { mode = it },
                    selectedPointId = selectedPointId,
                    runtimeSession = runtimeSession,
                    speedText = speedText,
                    pointLatInput = pointLatInput,
                    pointLngInput = pointLngInput,
                    isServiceRunning = isServiceRunning,
                    isServicePaused = isServicePaused,
                    onPointLatChange = onPointLatChange,
                    onPointLngChange = onPointLngChange,
                    onSpeedTextChange = onSpeedTextChange,
                    onOpenPointPicker = onOpenPointPicker,
                    onOpenRouteEditor = onOpenRouteEditor,
                    onStartPoint = onStartPoint,
                    onStartRoute = onStartRoute,
                    onPause = onPause,
                    onResumeMock = onResumeMock,
                    onStop = onStop
                ) }
            }
            if (mode == SimulationMode.ROUTE) item {
                androidx.compose.foundation.layout.Box(animateItem()) {
                SavedRoutesSection(
                    routes = routes,
                    selectedRouteId = selectedRouteId,
                    speedText = speedText,
                    onStartRoute = onStartRoute,
                    onEditRoute = onEditRoute,
                    onDeleteRoute = onDeleteRoute,
                    onSelectRoute = { selectedRouteId = it.id }
                ) }
            }
            if (mode == SimulationMode.POINT) item {
                androidx.compose.foundation.layout.Box(animateItem()) {
                    SavedPointsSection(savedPoints, selectedPointId, onPointLatChange, onPointLngChange, onEditPoint, { id ->
                        if (selectedPointId == id) selectedPointId = savedPoints.firstOrNull { it.id != id }?.id
                        onDeletePoint(id)
                    }) { selectedPointId = it }
                }
            }
            item {
                androidx.compose.foundation.layout.Box(animateItem()) {
                NfcToolsRow(
                    isActivated = isNfcActivated,
                    onOpenAlipay = onOpenAlipayNfc,
                    onVerify = onVerifyNfc
                ) }
            }
        }
    }
}
@Composable
private fun SavedPointsSection(points: List<SavedPoint>, selectedId: String?, onLatChange: (String) -> Unit, onLngChange: (String) -> Unit, onEdit: (SavedPoint) -> Unit, onDelete: (String) -> Unit, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
        HomeSectionTitle("保存点位", points.size)
        if (points.isEmpty()) {
            Text("暂无保存点位，请通过“地图选点”添加", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        points.forEach { point ->
            key(point.id) {
            var confirmDelete by remember { mutableStateOf(false) }
            SavedRecordRow(
                record = PointRecord(point, "${formatCoordinate(point.point.latWgs84)}, ${formatCoordinate(point.point.lngWgs84)}"),
                selected = point.id == selectedId,
                onSelect = {
                    onSelect(point.id)
                    onLatChange(formatCoordinate(point.point.latWgs84))
                    onLngChange(formatCoordinate(point.point.lngWgs84))
                },
                menuContent = { dismiss ->
                    androidx.compose.material3.DropdownMenuItem(text = { Text("编辑位置") }, onClick = { dismiss(); onEdit(point) })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("删除") }, onClick = { dismiss(); confirmDelete = true })
                }
            )
            if (confirmDelete) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("删除点位") },
                    text = { Text("确认删除“${point.name}”？") },
                    confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false; onDelete(point.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
                )
            }
            }
        }
    }
}

/** Lightweight section title used across the home flow (Linear-style). */
@Composable
internal fun HomeSectionTitle(text: String, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (count != null) {
            Spacer(Modifier.width(RouteVergeSpacing.sm))
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
