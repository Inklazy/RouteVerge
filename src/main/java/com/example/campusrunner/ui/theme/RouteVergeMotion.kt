package com.example.campusrunner.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Shared, deliberately quiet motion tokens for the RouteVerge interface. */
object RouteVergeMotion {
    const val buttonPressDuration = 160
    const val contentDuration = 180
    const val listDuration = 220
    /** The point/route capsule and its paired mode-switch haptic share this token. */
    const val modeSelectorDuration = 220

    fun <T> tweenSpec(
        durationMillis: Int = contentDuration,
        easing: Easing = FastOutSlowInEasing
    ): FiniteAnimationSpec<T> = tween(durationMillis, easing = easing)

    fun <T> spec(reducedMotion: Boolean, durationMillis: Int = contentDuration): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tweenSpec(durationMillis)
}

/** Honors Android's system animator scale (including “Remove animations”). */
@Composable
fun rememberRouteVergeReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}
