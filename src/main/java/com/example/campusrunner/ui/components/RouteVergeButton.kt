package com.example.campusrunner.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeMotion
import com.example.campusrunner.ui.theme.rememberRouteVergeReducedMotion

/**
 * RouteVerge button — wraps Material 3 buttons with the RouteVerge look.
 *
 * Hierarchy (DESIGN.md §13/§14):
 *  - [RouteVergeButtonVariant.Filled] = primary action (brand primary)
 *  - [RouteVergeButtonVariant.Tonal]  = important secondary
 *  - [RouteVergeButtonVariant.Outlined] = secondary alternative
 *  - [RouteVergeButtonVariant.Text]   = low priority
 *
 * The wrapper only decides appearance (shape, min height, padding, icon
 * spacing, loading); screens decide content and layout.
 */
enum class RouteVergeButtonVariant { Filled, Tonal, Outlined, Text, DestructiveText }

object RouteVergeButtonDefaults {
    /** DESIGN.md §29 — comfortable touch target. */
    val MinHeight: Dp = 48.dp

    val ContentPadding: PaddingValues = PaddingValues(
        horizontal = RouteVergeSpacing.xl,
        vertical = RouteVergeSpacing.sm
    )

    val LoadingSize: Dp = 18.dp
}

@Composable
fun RouteVergeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: RouteVergeButtonVariant = RouteVergeButtonVariant.Filled,
    loading: Boolean = false,
    contentPadding: PaddingValues = RouteVergeButtonDefaults.ContentPadding,
    shape: Shape? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberRouteVergeReducedMotion()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.96f else 1f,
        animationSpec = RouteVergeMotion.spec(reducedMotion, RouteVergeMotion.buttonPressDuration),
        label = "button_press_scale"
    )
    val buttonModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .defaultMinSize(minHeight = RouteVergeButtonDefaults.MinHeight)
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            // Loading keeps the label visible: [spinner] text
            CircularProgressIndicator(
                modifier = Modifier.size(RouteVergeButtonDefaults.LoadingSize),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
            Spacer(Modifier.width(RouteVergeSpacing.sm))
            text()
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(RouteVergeSpacing.sm))
            }
            text()
            if (trailingIcon != null) {
                Spacer(Modifier.width(RouteVergeSpacing.sm))
                trailingIcon()
            }
        }
    }
    // Loading disables interaction: no repeated clicks, a11y reads it as disabled.
    val interactive = enabled && !loading
    when (variant) {
        RouteVergeButtonVariant.Filled -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.pill,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.Tonal -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.large,
            contentPadding = contentPadding,
            colors = ButtonDefaults.filledTonalButtonColors(),
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.large,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = interactive,
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.DestructiveText -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = interactive,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            interactionSource = interactionSource,
            content = content
        )
    }
}
