package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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

/**
 * Home destination — fixed simulation controls with a bounded saved-record list.
 * The NFC tool bar is owned by Scaffold.bottomBar, so it never participates in
 * record scrolling.
 * No card stacks, no dashboard. Technical state lives in the status detail
 * (Phase 9 Settings), not on the first screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    hasLocationPermission: Boolean,
    canMockLocation: Boolean,
    routes: List<SavedRoute>,
    savedPoints: List<SavedPoint>,
    mapProvider: MapProvider,
    selectedRouteId: String?,
    selectedPointId: String?,
    mode: SimulationMode,
    pointRecordsState: LazyListState,
    routeRecordsState: LazyListState,
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
    onModeChange: (SimulationMode) -> Unit,
    onSelectedRouteChange: (String?) -> Unit,
    onSelectedPointChange: (String?) -> Unit,
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
    // Selection and scroll state are owned by AppRoot so navigating to an
    // editor does not reset the previous Home presentation on return.
    androidx.compose.runtime.LaunchedEffect(routes) {
        if (routes.none { it.id == selectedRouteId }) onSelectedRouteChange(routes.firstOrNull()?.id)
    }
    androidx.compose.runtime.LaunchedEffect(savedPoints) {
        if (savedPoints.none { it.id == selectedPointId }) onSelectedPointChange(savedPoints.firstOrNull()?.id)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Keep the NFC launcher fixed to the app window. Removing it while
            // the IME is visible prevents it from being measured above the
            // keyboard and squeezing the simulation controls.
            if (WindowInsets.ime.getBottom(LocalDensity.current) == 0) {
                NfcToolsRow(
                    isActivated = isNfcActivated,
                    onOpenAlipay = onOpenAlipayNfc,
                    onVerify = onVerifyNfc
                )
            }
        },
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = RouteVergeSpacing.lg),
        ) {
            val mapHeight = responsiveMapHeight(maxHeight, LocalDensity.current.fontScale)
            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            Column(
                modifier = if (imeVisible) {
                    // Preserve designed control heights; let the page move
                    // instead of compressing buttons when the IME resizes us.
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier.fillMaxSize()
                },
                verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.lg)
            ) {
                HomeStatusSection(
                    hasLocationPermission = hasLocationPermission,
                    canMockLocation = canMockLocation,
                    isServiceRunning = isServiceRunning,
                    isServicePaused = isServicePaused,
                    isNfcActivated = isNfcActivated,
                    onRequestPermissions = onRequestPermissions,
                    onOpenDeveloperOptions = onOpenDeveloperOptions,
                    onVerifyNfc = onVerifyNfc
                )
                SimulationSection(
                    routes = routes,
                    savedPoints = savedPoints,
                    mapProvider = mapProvider,
                    selectedRouteId = selectedRouteId,
                    mode = mode,
                    onModeChange = onModeChange,
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
                    onStop = onStop,
                    mapHeight = mapHeight
                )
                if (mode == SimulationMode.ROUTE) {
                    HomeSectionTitle("保存路线", routes.size)
                    SavedRoutesList(
                        modifier = if (imeVisible) Modifier.fillMaxWidth().heightIn(max = 200.dp)
                        else Modifier.fillMaxWidth().weight(1f),
                        listState = routeRecordsState,
                        routes = routes,
                        selectedRouteId = selectedRouteId,
                        speedText = speedText,
                        onStartRoute = onStartRoute,
                        onEditRoute = onEditRoute,
                        onDeleteRoute = onDeleteRoute,
                        onSelectRoute = { onSelectedRouteChange(it.id) }
                    )
                } else {
                    HomeSectionTitle("保存点位", savedPoints.size)
                    SavedPointsList(
                        modifier = if (imeVisible) Modifier.fillMaxWidth().heightIn(max = 200.dp)
                        else Modifier.fillMaxWidth().weight(1f),
                        listState = pointRecordsState,
                        points = savedPoints,
                        selectedId = selectedPointId,
                        onLatChange = onPointLatChange,
                        onLngChange = onPointLngChange,
                        onEdit = onEditPoint,
                        onDelete = { id ->
                        if (selectedPointId == id) onSelectedPointChange(savedPoints.firstOrNull { it.id != id }?.id)
                        onDeletePoint(id)
                        },
                        onSelect = onSelectedPointChange
                    )
                }
            }
        }
    }
}

/** Leaves a usable bounded history viewport on compact displays and large text. */
private fun responsiveMapHeight(availableHeight: androidx.compose.ui.unit.Dp, fontScale: Float) = when {
    availableHeight < 560.dp || fontScale >= 1.35f -> 112.dp
    availableHeight < 640.dp || fontScale >= 1.15f -> 144.dp
    availableHeight < 720.dp -> 176.dp
    else -> 220.dp
}

@Composable
private fun SavedPointsList(
    modifier: Modifier,
    listState: LazyListState,
    points: List<SavedPoint>,
    selectedId: String?,
    onLatChange: (String) -> Unit,
    onLngChange: (String) -> Unit,
    onEdit: (SavedPoint) -> Unit,
    onDelete: (String) -> Unit,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs)
    ) {
        if (points.isEmpty()) {
            item {
                Text(
                    "暂无保存点位，请通过“地图选点”添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(points, key = { it.id }) { point ->
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
