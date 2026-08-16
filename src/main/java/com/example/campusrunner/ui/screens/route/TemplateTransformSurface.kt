package com.example.campusrunner.ui.screens.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeCard
import com.example.campusrunner.ui.components.RouteVergeCardVariant
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonDefaults
import com.example.campusrunner.ui.theme.RouteVergeSpacing

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
    RouteVergeCard(
        variant = RouteVergeCardVariant.Elevated,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(
                top = RouteVergeSpacing.md + RouteVergeIconButtonDefaults.TouchTarget + RouteVergeSpacing.xs,
                end = RouteVergeSpacing.md
            )
    ) {
        Column(
            modifier = Modifier.padding(RouteVergeSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RouteVergeIconButton(onClick = onRotateLeft, contentDescription = "向左旋转") {
                    Icon(Icons.AutoMirrored.Rounded.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                RouteVergeIconButton(onClick = onRotateRight, contentDescription = "向右旋转") {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                RouteVergeIconButton(onClick = onScaleDown, contentDescription = "缩小") {
                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                RouteVergeIconButton(onClick = onScaleUp, contentDescription = "放大") {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            RouteVergeButton(
                onClick = onMoveToCenter,
                variant = RouteVergeButtonVariant.Outlined,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                leadingIcon = { Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp)) },
                text = { Text("移动到地图中心") }
            )
        }
    }
}
