package com.inklazy.routeverge.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteVergePressFeedbackTest {
    @Test
    fun releasedStateRestoresOriginalColor() {
        val base = Color(0xFFE6D6C7)
        assertEquals(base, RouteVergePressFeedback.color(base, Color.Black, pressed = false))
    }

    @Test
    fun pressedStateCoversTransparentAndOpaqueContainers() {
        val content = Color(0xFF473B32)
        val layer = content.copy(alpha = RouteVergePressFeedback.StateLayerAlpha)
        assertEquals(layer, RouteVergePressFeedback.color(Color.Transparent, content, pressed = true))
        assertEquals(
            layer.compositeOver(Color(0xFFE6D6C7)),
            RouteVergePressFeedback.color(Color(0xFFE6D6C7), content, pressed = true)
        )
    }
}
