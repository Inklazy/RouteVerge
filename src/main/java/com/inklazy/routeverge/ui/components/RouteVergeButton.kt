package com.inklazy.routeverge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import com.inklazy.routeverge.ui.theme.RouteVergeShapes
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeTheme
import com.inklazy.routeverge.ui.theme.rememberRouteVergeReducedMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RouteVerge button. Visible-container variants use one full-surface pressed state;
 * text variants keep Material's native indication.
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

/** Only primary simulation start/stop controls opt into this behavior. */
enum class RouteVergeButtonHapticFeedback { HighPriority }

@Composable
fun RouteVergeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: RouteVergeButtonVariant = RouteVergeButtonVariant.Filled,
    loading: Boolean = false,
    hapticFeedback: RouteVergeButtonHapticFeedback? = null,
    contentPadding: PaddingValues = RouteVergeButtonDefaults.ContentPadding,
    shape: Shape? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    val latestOnClick by rememberUpdatedState(onClick)
    val highPriorityHaptics = hapticFeedback == RouteVergeButtonHapticFeedback.HighPriority
    var lastActionUptime by remember { mutableStateOf(0L) }
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
    val debouncedOnClick = {
        val now = SystemClock.uptimeMillis()
        // The service has its own state guard; this additionally prevents a
        // second UI click before the first state change is drawn.
        if (!highPriorityHaptics || now - lastActionUptime >= RAPID_ACTION_GUARD_MILLIS) {
            lastActionUptime = now
            latestOnClick()
        }
    }
    if (highPriorityHaptics) {
        HighPriorityButtonHaptics(
            interactionSource = interactionSource,
            enabled = interactive,
            view = view
        )
    }
    val buttonModifier = modifier.defaultMinSize(minHeight = RouteVergeButtonDefaults.MinHeight)
    if (variant == RouteVergeButtonVariant.Text || variant == RouteVergeButtonVariant.DestructiveText) {
        // Text actions have no filled visual body. Keep Material's original
        // ripple, semantics and sizing; only filled controls need a solid layer.
        TextButton(
            onClick = debouncedOnClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.pill,
            colors = if (variant == RouteVergeButtonVariant.DestructiveText) {
                ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.textButtonColors()
            },
            interactionSource = interactionSource,
            content = content
        )
        return
    }

    val pressed by interactionSource.collectIsPressedAsState()
    val buttonScale = remember { Animatable(1f) }
    val reducedMotion = rememberRouteVergeReducedMotion()
    LaunchedEffect(interactionSource, interactive, reducedMotion) {
        if (!interactive) {
            buttonScale.snapTo(1f)
            return@LaunchedEffect
        }
        var scaleJob: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    scaleJob?.cancel()
                    scaleJob = launch {
                        if (reducedMotion) buttonScale.snapTo(PRESSED_BUTTON_SCALE)
                        else buttonScale.animateTo(
                            PRESSED_BUTTON_SCALE,
                            animationSpec = tween(
                                durationMillis = BUTTON_PRESS_SCALE_DURATION_MILLIS,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    scaleJob?.cancel()
                    scaleJob = launch {
                        // A brief tap may end before the press tween has drawn a frame.
                        // Still show the full pressed scale before springing back.
                        if (reducedMotion) buttonScale.snapTo(1f)
                        else {
                            buttonScale.snapTo(PRESSED_BUTTON_SCALE)
                            buttonScale.animateTo(
                                1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    val resolvedShape = shape ?: if (variant == RouteVergeButtonVariant.Filled) {
        RouteVergeShapes.pill
    } else {
        RouteVergeShapes.large
    }
    val scheme = MaterialTheme.colorScheme
    val containerColor = when (variant) {
        RouteVergeButtonVariant.Filled -> scheme.primary
        RouteVergeButtonVariant.Tonal -> scheme.secondaryContainer
        else -> scheme.background
    }
    val contentColor = when (variant) {
        RouteVergeButtonVariant.Filled -> scheme.onPrimary
        RouteVergeButtonVariant.Tonal -> scheme.onSecondaryContainer
        else -> scheme.onBackground
    }
    val displayedContainer = when {
        !interactive && variant == RouteVergeButtonVariant.Filled -> scheme.surfaceContainerHighest
        !interactive && variant == RouteVergeButtonVariant.Tonal -> scheme.onSurface.copy(alpha = 0.12f)
        !interactive -> containerColor
        pressed && variant == RouteVergeButtonVariant.Filled -> RouteVergeTheme.palette.primaryPressed
        else -> RouteVergePressFeedback.color(containerColor, contentColor, pressed)
    }
    val displayedContent = if (interactive) contentColor else if (variant == RouteVergeButtonVariant.Filled) {
        scheme.onSurfaceVariant
    } else {
        scheme.onSurface.copy(alpha = 0.38f)
    }
    val border = if (variant == RouteVergeButtonVariant.Outlined) {
        BorderStroke(1.dp, if (interactive) scheme.outline else scheme.onSurface.copy(alpha = 0.12f))
    } else {
        null
    }

    // The visual Surface and clickable cover exactly the same bounds. There
    // is no Material ripple underneath the full-container pressed color.
    Surface(
        modifier = buttonModifier
            .defaultMinSize(minWidth = ButtonDefaults.MinWidth)
            // A graphics layer scales the whole Surface without changing its layout bounds.
            .graphicsLayer { scaleX = buttonScale.value; scaleY = buttonScale.value }
            .clip(resolvedShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = interactive,
                role = Role.Button,
                onClick = debouncedOnClick
            ),
        shape = resolvedShape,
        color = displayedContainer,
        contentColor = displayedContent,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                content()
            }
        }
    }
}

@Composable
private fun HighPriorityButtonHaptics(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    view: android.view.View
) {
    LaunchedEffect(interactionSource, enabled, view) {
        var activePress: PressInteraction.Press? = null
        var pressStartedAt = 0L
        var longPressTriggered = false
        var longPressJob: Job? = null
        var lastShortFeedbackAt = 0L

        fun performShortFeedback() {
            val now = SystemClock.uptimeMillis()
            if (now - lastShortFeedbackAt >= RAPID_HAPTIC_GUARD_MILLIS) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                lastShortFeedbackAt = now
            }
        }

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    longPressJob?.cancel()
                    activePress = interaction
                    pressStartedAt = SystemClock.uptimeMillis()
                    longPressTriggered = false
                    longPressJob = launch {
                        delay(LONG_PRESS_THRESHOLD_MILLIS)
                        if (activePress === interaction) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            longPressTriggered = true
                        }
                    }
                }

                is PressInteraction.Release -> if (interaction.press === activePress) {
                    longPressJob?.cancel()
                    val wasLongPress = longPressTriggered ||
                        SystemClock.uptimeMillis() - pressStartedAt >= LONG_PRESS_THRESHOLD_MILLIS
                    // If the main thread reached release before the delayed
                    // coroutine got a frame, preserve the >= 250ms contract.
                    if (wasLongPress && !longPressTriggered) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    if (wasLongPress) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } else {
                        performShortFeedback()
                    }
                    activePress = null
                    longPressJob = null
                }

                is PressInteraction.Cancel -> if (interaction.press === activePress) {
                    longPressJob?.cancel()
                    activePress = null
                    longPressJob = null
                    longPressTriggered = false
                }
            }
        }
    }
}

private const val PRESSED_BUTTON_SCALE = 0.985f
private const val BUTTON_PRESS_SCALE_DURATION_MILLIS = 90
private const val LONG_PRESS_THRESHOLD_MILLIS = 250L
private const val RAPID_ACTION_GUARD_MILLIS = 350L
private const val RAPID_HAPTIC_GUARD_MILLIS = 150L
