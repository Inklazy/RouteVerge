package com.example.campusrunner.ui.screens.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.data.RoutePoint
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.geo.buildTrackOval
import com.example.campusrunner.ui.components.RouteVergeStatus
import com.example.campusrunner.ui.components.RouteVergeTextField
import com.example.campusrunner.ui.formatDistance
import com.example.campusrunner.ui.map.MapController
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/**
 * Route drawing editor. Modes are mutually exclusive:
 *  - IDLE (empty): start drawing
 *  - DRAWING: undo / finish drawing
 *  - ROUTE (open/closed): continue drawing / undo / save + overflow
 *  - TEMPLATE: transform + cancel / apply
 * Drawing and template never appear together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditorScreen(
    mapProvider: MapProvider,
    onMapProviderChange: (MapProvider) -> Unit,
    runtimeEntryText: String? = null,
    runtimeEntryStatus: RouteVergeStatus? = null,
    onRuntimeEntryClick: (() -> Unit)? = null,
    initialRoute: SavedRoute?,
    closeLoop: Boolean,
    loopCountText: String,
    onCloseLoopChange: (Boolean) -> Unit,
    onLoopCountTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onLocateMe: () -> RoutePoint?,
    onSaveRoute: (String, List<RoutePoint>) -> Unit
) {
    val points = remember(initialRoute?.id) {
        mutableStateListOf<RoutePoint>().apply {
            addAll(initialRoute?.points.orEmpty())
        }
    }
    var pointsVersion by remember(initialRoute?.id) { mutableStateOf(0) }
    var name by remember(initialRoute?.id) { mutableStateOf(initialRoute?.name ?: "") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showNameError by remember { mutableStateOf(false) }
    var showLoopSheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var drawMode by remember { mutableStateOf(false) }
    var templateEnabled by remember(initialRoute?.id) { mutableStateOf(false) }
    var templateCenter by remember(initialRoute?.id) { mutableStateOf<RoutePoint?>(null) }
    var templateLength by remember(initialRoute?.id) { mutableStateOf(160.0) }
    var templateWidth by remember(initialRoute?.id) { mutableStateOf(75.0) }
    var templateRotation by remember(initialRoute?.id) { mutableStateOf(0.0) }

    val templatePoints = templateCenter?.let { center ->
        buildTrackOval(center, templateLength, templateWidth, templateRotation)
    }.orEmpty()

    val undoPoints: () -> Unit = {
        val removeCount = points.size.coerceAtMost(8)
        repeat(removeCount) { points.removeLast() }
        pointsVersion++
    }

    val enterDrawing: () -> Unit = {
        templateEnabled = false
        drawMode = true
    }

    val enterTemplate: (MapController) -> Unit = { controller ->
        templateCenter = controller.cameraTarget() ?: initialRoute?.points?.firstOrNull() ?: RoutePoint(39.9042, 116.4074)
        drawMode = false
        templateEnabled = true
    }

    MapSelectionScreen(
        title = "绘制路线",
        mapProvider = mapProvider,
        onMapProviderChange = onMapProviderChange,
        runtimeEntryText = runtimeEntryText,
        runtimeEntryStatus = runtimeEntryStatus,
        onRuntimeEntryClick = onRuntimeEntryClick,
        initialPoint = initialRoute?.points?.firstOrNull(),
        points = points,
        pointsVersion = pointsVersion,
        closeLoopPreview = closeLoop,
        drawMode = drawMode,
        onBack = onBack,
        onLocateMe = onLocateMe,
        onUndo = null,
        onDrawPoint = { point ->
            val last = points.lastOrNull()
            if (last == null || RouteMath.distanceMeters(last, point) >= 4.0) {
                points.add(point)
                pointsVersion++
            }
        },
        overlayContent = { controller ->
            if (templateEnabled && templateCenter != null) {
                TemplateTransformSurface(
                    onRotateLeft = { templateRotation += 5.0 },
                    onRotateRight = { templateRotation -= 5.0 },
                    onScaleUp = {
                        templateLength = (templateLength * 1.08).coerceAtMost(400.0)
                        templateWidth = (templateWidth * 1.08).coerceAtMost(160.0)
                    },
                    onScaleDown = {
                        templateLength = (templateLength * 0.92).coerceAtLeast(40.0)
                        templateWidth = (templateWidth * 0.92).coerceAtLeast(20.0)
                    },
                    onMoveToCenter = { templateCenter = controller.cameraTarget() ?: templateCenter }
                )
            }
        },
        bottomContent = { controller ->
            RouteEditorBottomSurface(
                pointCount = points.size,
                distanceText = formatDistance(RouteMath.totalDistanceMeters(points, closeLoop)),
                drawMode = drawMode,
                templateEnabled = templateEnabled,
                closeLoop = closeLoop,
                loopCountText = loopCountText,
                canSave = points.size >= 2,
                onStartDrawing = enterDrawing,
                onDoneDrawing = { drawMode = false },
                onContinueDrawing = enterDrawing,
                onUndo = undoPoints,
                onSave = { showSaveDialog = true },
                onOpenLoop = { showLoopSheet = true },
                onOpenTemplate = { enterTemplate(controller) },
                onClear = { showClearConfirm = true },
                onCancelTemplate = { templateEnabled = false },
                onApplyTemplate = {
                    if (templatePoints.size >= 2) {
                        points.clear()
                        points.addAll(templatePoints)
                        onCloseLoopChange(true)
                        templateEnabled = false
                        drawMode = false
                        pointsVersion++
                    }
                }
            )
        },
        showTopTitle = false,
        previewPoints = if (templateEnabled) templatePoints else emptyList()
    )

    if (showLoopSheet) {
        LoopSettingsSheet(
            closeLoop = closeLoop,
            loopCountText = loopCountText,
            onCloseLoopChange = onCloseLoopChange,
            onLoopCountTextChange = onLoopCountTextChange,
            onDismiss = { showLoopSheet = false }
        )
    }

    if (showClearConfirm) {
        ClearRouteDialog(
            onConfirm = {
                points.clear()
                pointsVersion++
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存路线") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
                    RouteVergeTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (showNameError) showNameError = false
                        },
                        label = "路线名称",
                        singleLine = true,
                        isError = showNameError,
                        supportingText = if (showNameError) "请输入路线名称" else null
                    )
                    Text(if (closeLoop) "模式：闭环 " + (loopCountText.toIntOrNull() ?: 1) + " 圈" else "模式：非闭环往返", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("同名路线会被覆盖。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                    } else {
                        showSaveDialog = false
                        onSaveRoute(name, points.toList())
                    }
                }) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(RouteVergeSpacing.xs))
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoopSettingsSheet(
    closeLoop: Boolean,
    loopCountText: String,
    onCloseLoopChange: (Boolean) -> Unit,
    onLoopCountTextChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Local editing state: only valid values (>= 1) propagate to the app
        // state, so the shown value always equals what gets saved.
        var localCount by remember { mutableStateOf(loopCountText) }
        val parsedCount = localCount.trim().toIntOrNull()
        val countValid = parsedCount != null && parsedCount >= 1

        LaunchedEffect(localCount) {
            if (countValid) {
                onLoopCountTextChange(localCount.trim())
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RouteVergeSpacing.lg)
                .padding(bottom = RouteVergeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)
        ) {
            Text("闭环设置", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "首尾闭环",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = closeLoop, onCheckedChange = onCloseLoopChange)
            }
            if (closeLoop) {
                RouteVergeTextField(
                    value = localCount,
                    onValueChange = { localCount = it },
                    label = "圈数",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = !countValid,
                    supportingText = if (!countValid) "圈数至少为 1" else null
                )
            }
        }
    }
}

@Composable
private fun ClearRouteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清空路线？") },
        text = { Text("这会移除当前未保存的所有路线点。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("清空", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
