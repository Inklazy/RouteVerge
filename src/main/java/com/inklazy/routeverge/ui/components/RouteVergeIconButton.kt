package com.inklazy.routeverge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inklazy.routeverge.ui.theme.RouteVergeTheme

/**
 * RouteVerge compact icon button.
 *
 * Visual size and touch target are decoupled (DESIGN.md §29):
 * the visual circle stays compact while the touch area is always 48dp.
 * All colors come from the theme — callers never hard-code colors.
 *
 * Variants (container / default content):
 *  - [RouteVergeIconButtonVariant.Plain]    transparent / onSurfaceVariant (quiet, e.g. top bar)
 *  - [RouteVergeIconButtonVariant.Floating] surfaceContainerHigh / onSurface (map floating control)
 *  - [RouteVergeIconButtonVariant.Selected] primary / onPrimary (active / toggled)
 *
 * [danger] forces the error color for destructive actions (delete / close).
 */
enum class RouteVergeIconButtonVariant { Plain, Floating, Selected }

object RouteVergeIconButtonDefaults {
    /** Minimum touch target (DESIGN.md §29). */
    val TouchTarget: Dp = 48.dp

    /** Compact visual size inside the touch target. */
    val VisualSize: Dp = 40.dp
}

@Composable
fun RouteVergeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: RouteVergeIconButtonVariant = RouteVergeIconButtonVariant.Floating,
    danger: Boolean = false,
    contentDescription: String? = null,
    contentColor: Color? = null,
    visualSize: Dp = RouteVergeIconButtonDefaults.VisualSize,
    content: @Composable () -> Unit
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
        variant == RouteVergeIconButtonVariant.Selected -> MaterialTheme.colorScheme.primary
        variant == RouteVergeIconButtonVariant.Floating -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }
    val resolvedContentColor = when {
        !enabled -> RouteVergeTheme.palette.textDisabled
        danger -> MaterialTheme.colorScheme.error
        variant == RouteVergeIconButtonVariant.Selected -> MaterialTheme.colorScheme.onPrimary
        variant == RouteVergeIconButtonVariant.Floating -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }.let { contentColor ?: it }

    val isSelected = enabled && variant == RouteVergeIconButtonVariant.Selected

    Box(
        modifier = modifier
            .size(RouteVergeIconButtonDefaults.TouchTarget)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                this.selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = containerColor,
            contentColor = resolvedContentColor,
            shape = CircleShape,
            modifier = Modifier.size(visualSize)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}
