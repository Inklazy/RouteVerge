package com.inklazy.routeverge.ui.screens.route

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inklazy.routeverge.data.MapProvider
import com.inklazy.routeverge.data.RoutePoint
import com.inklazy.routeverge.geo.RouteMath
import com.inklazy.routeverge.ui.components.RouteVergeCard
import com.inklazy.routeverge.ui.components.RouteVergeCardVariant
import com.inklazy.routeverge.ui.components.RouteVergeIconButton
import com.inklazy.routeverge.ui.components.RouteVergeIconButtonDefaults
import com.inklazy.routeverge.ui.components.RouteVergeStatus
import com.inklazy.routeverge.ui.components.StatusDot
import com.inklazy.routeverge.ui.formatDistance
import com.inklazy.routeverge.ui.map.CampusMapView
import com.inklazy.routeverge.ui.map.MapController
import com.inklazy.routeverge.ui.map.renderRoute
import com.inklazy.routeverge.ui.theme.RouteVergeShapes
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeMapTokens
import kotlin.math.roundToInt

/**
 * Shared map container for point picking and route drawing.
 * Owns the map lifecycle, floating top controls and the bottom action area;
 * the caller provides the business overlays and the bottom content.
 */
@Composable
fun MapSelectionScreen(
    title: String,
    mapProvider: MapProvider,
    onMapProviderChange: (MapProvider) -> Unit,
    initialPoint: RoutePoint?,
    points: List<RoutePoint>,
    pointsVersion: Int,
    closeLoopPreview: Boolean,
    drawMode: Boolean,
    onBack: () -> Unit,
    onLocateMe: () -> RoutePoint?,
    onUndo: (() -> Unit)?,
    onDrawPoint: ((RoutePoint) -> Unit)?,
    overlayContent: @Composable BoxScope.(MapController) -> Unit,
    previewPoints: List<RoutePoint>,
    bottomContent: @Composable (MapController) -> Unit,
    showTopTitle: Boolean = true,
    runtimeEntryText: String? = null,
    runtimeEntryStatus: RouteVergeStatus? = null,
    onRuntimeEntryClick: (() -> Unit)? = null
) {
    var mapController by remember { mutableStateOf<MapController?>(null) }
    var satelliteEnabled by remember { mutableStateOf(false) }
    var optionsExpanded by remember { mutableStateOf(false) }
    // Camera target captured right before a provider switch, restored when the
    // new provider's map is ready (position only; zoom continuity out of scope).
    var pendingCameraTarget by remember { mutableStateOf<RoutePoint?>(null) }

    LaunchedEffect(mapController, pointsVersion, closeLoopPreview, previewPoints, drawMode) {
        val controller = mapController ?: return@LaunchedEffect
        renderRoute(controller, points, closeLoopPreview, previewPoints, drawMode)
    }

    LaunchedEffect(mapController, satelliteEnabled) {
        mapController?.setSatelliteEnabled(satelliteEnabled)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CampusMapView(
            provider = mapProvider,
            modifier = Modifier.fillMaxSize()
        ) { controller ->
            mapController = controller
            controller.disableUiControls()
            controller.setSatelliteEnabled(satelliteEnabled)
            controller.moveCamera(pendingCameraTarget ?: initialPoint ?: RoutePoint(39.9042, 116.4074), 16f)
            renderRoute(controller, points, closeLoopPreview, previewPoints, drawMode)
        }

        if (drawMode && onDrawPoint != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(mapController, drawMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                mapController
                                    ?.screenPointToRoutePoint(offset.x.roundToInt(), offset.y.roundToInt())
                                    ?.let(onDrawPoint)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                mapController
                                    ?.screenPointToRoutePoint(change.position.x.roundToInt(), change.position.y.roundToInt())
                                    ?.let(onDrawPoint)
                            }
                        )
                    }
            )
        } else {
            // Fixed screen-center selection target (point picker + route editor).
            MapCenterCrosshair()
        }

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(RouteVergeSpacing.md)
        ) {
            val showTopUndo = onUndo != null && points.isNotEmpty()
            val rightButtonCount = if (showTopUndo) 3 else 2
            val reservedWidth = RouteVergeIconButtonDefaults.TouchTarget * (rightButtonCount + 1) +
                RouteVergeSpacing.sm * (rightButtonCount + 1)
            val runtimeChipMaxWidth = (maxWidth - reservedWidth).coerceAtLeast(RouteVergeIconButtonDefaults.TouchTarget)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
            ) {
                RouteVergeIconButton(
                    onClick = onBack,
                    contentDescription = "返回"
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                if (runtimeEntryText != null && onRuntimeEntryClick != null) {
                    // Keep every map action fixed at 48dp; only the status chip yields width.
                    Box(modifier = Modifier.weight(1f)) {
                        RuntimeEntryChip(
                            text = runtimeEntryText,
                            status = runtimeEntryStatus,
                            onClick = onRuntimeEntryClick,
                            modifier = Modifier.widthIn(max = runtimeChipMaxWidth)
                        )
                    }
                } else if (showTopTitle) {
                    RouteVergeCard(
                        variant = RouteVergeCardVariant.Elevated,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (points.isEmpty()) title else "$title · ${points.size} 点 · ${formatDistance(RouteMath.totalDistanceMeters(points, closeLoopPreview))}",
                            modifier = Modifier.padding(horizontal = RouteVergeSpacing.lg, vertical = RouteVergeSpacing.md),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
                    AnimatedVisibility(showTopUndo) {
                        RouteVergeIconButton(
                            onClick = { onUndo?.invoke() },
                            contentDescription = "撤销"
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                        }
                    }
                    RouteVergeIconButton(
                        onClick = { optionsExpanded = true },
                        contentDescription = "地图选项"
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = null)
                    }
                    RouteVergeIconButton(
                        onClick = {
                            val current = onLocateMe()
                            if (current != null) {
                                mapController?.animateCamera(current, 17f)
                            }
                        },
                        contentDescription = "定位到当前位置",
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null)
                    }
                }
            }
        }

        mapController?.let { controller ->
            overlayContent(controller)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(RouteVergeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
        ) {
            val controller = mapController
            if (controller != null) {
                bottomContent(controller)
            }
        }
    }

    if (optionsExpanded) {
        MapOptionsSheet(
            mapProvider = mapProvider,
            satelliteEnabled = satelliteEnabled,
            onProviderChange = { provider ->
                pendingCameraTarget = mapController?.cameraTarget()
                onMapProviderChange(provider)
            },
            onSatelliteChange = { satelliteEnabled = it },
            onDismiss = { optionsExpanded = false }
        )
    }
}

/** Low-frequency map settings: source (auto / AMap / Google) and style (standard / satellite). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapOptionsSheet(
    mapProvider: MapProvider,
    satelliteEnabled: Boolean,
    onProviderChange: (MapProvider) -> Unit,
    onSatelliteChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RouteVergeShapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RouteVergeSpacing.lg)
                .padding(bottom = RouteVergeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs)
        ) {
            Text("地图选项", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(RouteVergeSpacing.sm))
            Text(
                "地图来源",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MapProvider.entries.forEach { provider ->
                OptionRow(
                    label = provider.label,
                    selected = mapProvider == provider,
                    onClick = { onProviderChange(provider) }
                )
            }
            Spacer(Modifier.height(RouteVergeSpacing.md))
            Text(
                "地图样式",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptionRow(
                label = "标准",
                selected = !satelliteEnabled,
                onClick = { onSatelliteChange(false) }
            )
            OptionRow(
                label = "卫星",
                selected = satelliteEnabled,
                onClick = { onSatelliteChange(true) }
            )
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = RouteVergeSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = null)
    }
}

/**
 * Compact non-home runtime entry: a status pill (running / paused dot + text),
 * tapping returns Home. It is a status entry, not a resume action.
 */
@Composable
private fun RuntimeEntryChip(
    text: String,
    status: RouteVergeStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RouteVergeShapes.pill,
        modifier = modifier
            .clip(RouteVergeShapes.pill)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "返回首页 · $text"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RouteVergeSpacing.md, vertical = RouteVergeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
        ) {
            if (status != null) {
                StatusDot(status = status)
            }
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BoxScope.MapCenterCrosshair() {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(RouteVergeMapTokens.crosshairSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(RouteVergeMapTokens.crosshairArmLength)
                .height(RouteVergeMapTokens.crosshairStrokeWidth)
                .shadow(RouteVergeMapTokens.crosshairShadowElevation, RectangleShape, ambientColor = RouteVergeMapTokens.crosshairShadow, spotColor = RouteVergeMapTokens.crosshairShadow)
                .background(RouteVergeMapTokens.crosshairForeground)
        )
        Box(
            Modifier
                .width(RouteVergeMapTokens.crosshairStrokeWidth)
                .height(RouteVergeMapTokens.crosshairArmLength)
                .shadow(RouteVergeMapTokens.crosshairShadowElevation, RectangleShape, ambientColor = RouteVergeMapTokens.crosshairShadow, spotColor = RouteVergeMapTokens.crosshairShadow)
                .background(RouteVergeMapTokens.crosshairForeground)
        )
    }
}
