package com.example.campusrunner.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeMotion
import com.example.campusrunner.ui.theme.rememberRouteVergeReducedMotion
import com.example.campusrunner.ui.theme.RouteVergeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val filledContainerColor = if (pressed && interactive) {
        RouteVergeTheme.palette.primaryPressed
    } else {
        MaterialTheme.colorScheme.primary
    }
    when (variant) {
        RouteVergeButtonVariant.Filled -> Button(
            onClick = debouncedOnClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.pill,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = filledContainerColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.Tonal -> Button(
            onClick = debouncedOnClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.large,
            contentPadding = contentPadding,
            colors = ButtonDefaults.filledTonalButtonColors(),
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.Outlined -> OutlinedButton(
            onClick = debouncedOnClick,
            modifier = buttonModifier,
            enabled = interactive,
            shape = shape ?: RouteVergeShapes.large,
            contentPadding = contentPadding,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.Text -> TextButton(
            onClick = debouncedOnClick,
            modifier = buttonModifier,
            enabled = interactive,
            interactionSource = interactionSource,
            content = content
        )

        RouteVergeButtonVariant.DestructiveText -> TextButton(
            onClick = debouncedOnClick,
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

private const val LONG_PRESS_THRESHOLD_MILLIS = 250L
private const val RAPID_ACTION_GUARD_MILLIS = 350L
private const val RAPID_HAPTIC_GUARD_MILLIS = 150L
