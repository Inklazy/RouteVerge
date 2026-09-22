package com.inklazy.routeverge.service

/** Small injectable clock abstraction for active simulation duration. */
fun interface MonotonicClock {
    fun elapsedRealtimeMillis(): Long
}

object SystemMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMillis(): Long = android.os.SystemClock.elapsedRealtime()
}

/**
 * Tracks active duration independently of wall-clock changes. The service
 * persists progress for live-session UI details, but each new service runtime
 * starts a fresh clock; the optional initial values are only for clock
 * construction/testing.
 */
class ActiveSimulationClock(
    private val clock: MonotonicClock = SystemMonotonicClock
) {
    private var startedAtMillis = 0L
    private var pausedAtMillis = 0L
    private var accumulatedPausedMillis = 0L
    private var paused = false

    fun start(initialActiveElapsedMillis: Long = 0L, initiallyPaused: Boolean = false) {
        val now = clock.elapsedRealtimeMillis()
        startedAtMillis = now - initialActiveElapsedMillis.coerceAtLeast(0L)
        pausedAtMillis = if (initiallyPaused) now else 0L
        accumulatedPausedMillis = 0L
        paused = initiallyPaused
    }

    fun pause() {
        if (!paused) {
            pausedAtMillis = clock.elapsedRealtimeMillis()
            paused = true
        }
    }

    fun resume() {
        if (paused) {
            accumulatedPausedMillis += clock.elapsedRealtimeMillis() - pausedAtMillis
            pausedAtMillis = 0L
            paused = false
        }
    }

    fun activeElapsedMillis(): Long {
        val pausedNow = if (paused && pausedAtMillis > 0L) {
            clock.elapsedRealtimeMillis() - pausedAtMillis
        } else {
            0L
        }
        return (clock.elapsedRealtimeMillis() - startedAtMillis - accumulatedPausedMillis - pausedNow)
            .coerceAtLeast(0L)
    }
}