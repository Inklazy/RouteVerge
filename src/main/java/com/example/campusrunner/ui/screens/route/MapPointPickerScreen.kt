package com.example.campusrunner.ui.screens.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.data.RoutePoint
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeStatus
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeIconSizes

/**
 * Pick a single point: move the map, keep the fixed center target on the
 * location you want, then confirm. Confirm returns the camera center —
 * Locate only moves the camera and is never a confirm action.
 */
@Composable
fun MapPointPickerScreen(
    mapProvider: MapProvider,
    onMapProviderChange: (MapProvider) -> Unit,
    runtimeEntryText: String? = null,
    runtimeEntryStatus: RouteVergeStatus? = null,
    onRuntimeEntryClick: (() -> Unit)? = null,
    startPoint: RoutePoint?,
    onBack: () -> Unit,
    onLocateMe: () -> RoutePoint?,
    onPointPicked: (RoutePoint) -> Unit
) {
    MapSelectionScreen(
        title = "定点选点",
        mapProvider = mapProvider,
        onMapProviderChange = onMapProviderChange,
        runtimeEntryText = runtimeEntryText,
        runtimeEntryStatus = runtimeEntryStatus,
        onRuntimeEntryClick = onRuntimeEntryClick,
        initialPoint = startPoint,
        points = emptyList(),
        pointsVersion = 0,
        closeLoopPreview = false,
        drawMode = false,
        onBack = onBack,
        onLocateMe = onLocateMe,
        onUndo = null,
        onDrawPoint = null,
        overlayContent = {},
        previewPoints = emptyList(),
        showTopTitle = false,
        bottomContent = { controller ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RouteVergeShapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(RouteVergeSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "移动地图以选择位置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RouteVergeButton(
                        onClick = { controller.cameraTarget()?.let(onPointPicked) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard)) },
                        text = { Text("确认此位置") }
                    )
                }
            }
        }
    )
}
