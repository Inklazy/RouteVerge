package com.example.campusrunner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeTheme

/**
 * Unified semantic status set (AGENTS.md §15). Color is never the only
 * indicator: badges always pair a label with the semantic color, and an
 * optional icon (default: status dot).
 */
enum class RouteVergeStatus { Ready, Running, Paused, Completed, Error, Warning, Disabled }

/** Vivid accent of a status — used for dots and icons. */
@Composable
@ReadOnlyComposable
private fun RouteVergeStatus.vividColor(): Color = when (this) {
    RouteVergeStatus.Ready, RouteVergeStatus.Completed -> RouteVergeTheme.statusColors.success
    RouteVergeStatus.Running -> MaterialTheme.colorScheme.primary
    RouteVergeStatus.Paused, RouteVergeStatus.Warning -> RouteVergeTheme.statusColors.warning
    RouteVergeStatus.Error -> MaterialTheme.colorScheme.error
    RouteVergeStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Soft container of a status — badge background. */
@Composable
@ReadOnlyComposable
private fun RouteVergeStatus.containerColor(): Color = when (this) {
    RouteVergeStatus.Ready, RouteVergeStatus.Completed -> RouteVergeTheme.statusColors.successContainer
    RouteVergeStatus.Running -> MaterialTheme.colorScheme.primaryContainer
    RouteVergeStatus.Paused, RouteVergeStatus.Warning -> RouteVergeTheme.statusColors.warningContainer
    RouteVergeStatus.Error -> RouteVergeTheme.statusColors.dangerContainer
    RouteVergeStatus.Disabled -> MaterialTheme.colorScheme.surfaceContainerHighest
}

/** Readable content color on top of [containerColor]. */
@Composable
@ReadOnlyComposable
private fun RouteVergeStatus.onContainerColor(): Color = when (this) {
    RouteVergeStatus.Ready, RouteVergeStatus.Completed -> RouteVergeTheme.statusColors.onSuccessContainer
    RouteVergeStatus.Running -> MaterialTheme.colorScheme.onPrimaryContainer
    RouteVergeStatus.Paused, RouteVergeStatus.Warning -> RouteVergeTheme.statusColors.onWarningContainer
    RouteVergeStatus.Error -> RouteVergeTheme.statusColors.onDangerContainer
    RouteVergeStatus.Disabled -> RouteVergeTheme.palette.textDisabled
}

/** Small status dot (decorative — pair it with a label or a described control). */
@Composable
fun StatusDot(
    status: RouteVergeStatus,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(status.vividColor(), CircleShape)
    )
}

/** Pill-shaped status badge: label + optional icon (default dot) on a soft container. */
@Composable
fun StatusBadge(
    label: String,
    status: RouteVergeStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge
) {
    Surface(
        color = status.containerColor(),
        contentColor = status.onContainerColor(),
        shape = RouteVergeShapes.pill,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = RouteVergeSpacing.md,
                vertical = RouteVergeSpacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
        ) {
            if (icon != null) {
                icon()
            } else {
                StatusDot(status)
            }
            Text(label, style = textStyle)
        }
    }
}
