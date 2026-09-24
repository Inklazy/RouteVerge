package com.inklazy.routeverge.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** Shared pressed-state math for custom visuals that keep a larger touch target. */
object RouteVergePressFeedback {
    /** Material-style state layer opacity, kept warm through the supplied content color. */
    const val StateLayerAlpha = 0.12f

    fun color(base: Color, content: Color, pressed: Boolean): Color =
        if (pressed) content.copy(alpha = StateLayerAlpha).compositeOver(base) else base
}
