package com.example.campusrunner.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * A narrowly-scoped fallback for the selector: Android's standard view haptics
 * have no duration argument, while this feedback needs to last exactly as long
 * as the selector capsule transition. Vibrating is asynchronous; no worker or
 * long-lived service is created here.
 */
@Composable
fun rememberModeSwitchHaptic(): ModeSwitchHaptic {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = remember(context, view) { ModeSwitchHaptic(context.modeSwitchVibrator(), view) }

    DisposableEffect(haptic, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) haptic.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            haptic.cancel()
        }
    }
    return haptic
}

class ModeSwitchHaptic internal constructor(
    private val vibrator: Vibrator?,
    private val view: android.view.View
) {
    /** Replacing a switch feedback prevents rapid mode changes from stacking. */
    fun perform(durationMillis: Int) {
        if (durationMillis <= 0) return
        val target = vibrator ?: return
        if (!target.hasVibrator()) return
        runCatching {
            target.cancel()
            target.vibrate(
                VibrationEffect.createOneShot(
                    durationMillis.toLong(),
                    MODE_SWITCH_AMPLITUDE
                )
            )
        }
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
    }

    /** The arrival confirmation uses the platform's normal short confirmation. */
    fun confirm() {
        runCatching {
            // Ensure the travel pulse cannot overlap the arrival pulse if the
            // vibrator scheduler is a few milliseconds behind the animation.
            vibrator?.cancel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    private companion object {
        // Deliberately softer than the system button feedback.
        const val MODE_SWITCH_AMPLITUDE = 32
    }
}

@Suppress("DEPRECATION")
private fun Context.modeSwitchVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
