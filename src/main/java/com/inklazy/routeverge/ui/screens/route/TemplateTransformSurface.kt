package com.inklazy.routeverge.ui.screens.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inklazy.routeverge.ui.components.RouteVergeIconButton
import com.inklazy.routeverge.ui.components.RouteVergeIconButtonDefaults
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeIconSizes
import com.inklazy.routeverge.ui.theme.RouteVergeShapes

/**
 * Floating template transform controls (screen-local to the route editor).
 * Compact 4-button transform row + "move to map center" action.
 */
@Composable
fun BoxScope.TemplateTransformSurface(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onScaleUp: () -> Unit,
    onScaleDown: () -> Unit,
    onMoveToCenter: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(
                top = RouteVergeSpacing.md + RouteVergeIconButtonDefaults.TouchTarget + RouteVergeSpacing.xs,
                start = RouteVergeSpacing.md,
                end = RouteVergeSpacing.md
            )
    ) {
        val toolbarSpacing = if (maxWidth < 280.dp) RouteVergeSpacing.xs else RouteVergeSpacing.sm
        Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
            shape = RouteVergeShapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                modifier = Modifier.padding(RouteVergeSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(toolbarSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RouteVergeIconButton(onClick = onRotateLeft, variant = com.inklazy.routeverge.ui.components.RouteVergeIconButtonVariant.Plain, contentDescription = "向左旋转") {
                    Icon(Icons.AutoMirrored.Rounded.RotateLeft, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard))
                }
                RouteVergeIconButton(onClick = onRotateRight, variant = com.inklazy.routeverge.ui.components.RouteVergeIconButtonVariant.Plain, contentDescription = "向右旋转") {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard))
                }
                RouteVergeIconButton(onClick = onScaleDown, variant = com.inklazy.routeverge.ui.components.RouteVergeIconButtonVariant.Plain, contentDescription = "缩小") {
                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard))
                }
                RouteVergeIconButton(onClick = onScaleUp, variant = com.inklazy.routeverge.ui.components.RouteVergeIconButtonVariant.Plain, contentDescription = "放大") {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard))
                }
                RouteVergeIconButton(onClick = onMoveToCenter, variant = com.inklazy.routeverge.ui.components.RouteVergeIconButtonVariant.Plain, contentDescription = "将跑道移动到地图中心") {
                    Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(RouteVergeIconSizes.standard))
                }
            }
        }
    }
}
