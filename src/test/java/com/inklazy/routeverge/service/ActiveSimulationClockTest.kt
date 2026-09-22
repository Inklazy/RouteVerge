package com.inklazy.routeverge.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveSimulationClockTest {
    private class FakeClock(var now: Long = 0L) : MonotonicClock {
        override fun elapsedRealtimeMillis(): Long = now
    }

    @Test
    fun activeTimeAdvancesFromMonotonicClock() {
        val clock = FakeClock()
        val sessionClock = ActiveSimulationClock(clock)
        sessionClock.start()
        clock.now = 1_500L
        assertEquals(1_500L, sessionClock.activeElapsedMillis())
    }

    @Test
    fun pauseAndResumeExcludePausedDuration() {
        val clock = FakeClock()
        val sessionClock = ActiveSimulationClock(clock)
        sessionClock.start()
        clock.now = 2_000L
        sessionClock.pause()
        clock.now = 8_000L
        assertEquals(2_000L, sessionClock.activeElapsedMillis())

        sessionClock.resume()
        clock.now = 9_500L
        assertEquals(3_500L, sessionClock.activeElapsedMillis())
    }

    @Test
    fun multiplePauseResumeCyclesAccumulateOnlyActiveTime() {
        val clock = FakeClock()
        val sessionClock = ActiveSimulationClock(clock)
        sessionClock.start()
        clock.now = 1_000L
        sessionClock.pause()
        clock.now = 3_000L
        sessionClock.resume()
        clock.now = 4_500L
        sessionClock.pause()
        clock.now = 10_000L
        sessionClock.resume()
        clock.now = 12_000L

        assertEquals(4_500L, sessionClock.activeElapsedMillis())
    }

    @Test
    fun initialDurationIsIndependentOfWallClockOrigin() {
        val clock = FakeClock(50_000L)
        val sessionClock = ActiveSimulationClock(clock)
        sessionClock.start(initialActiveElapsedMillis = 12_000L, initiallyPaused = false)
        clock.now = 52_500L

        assertEquals(14_500L, sessionClock.activeElapsedMillis())
    }

    @Test
    fun initiallyPausedSessionRemainsPausedUntilResume() {
        val clock = FakeClock(50_000L)
        val sessionClock = ActiveSimulationClock(clock)
        sessionClock.start(initialActiveElapsedMillis = 12_000L, initiallyPaused = true)
        clock.now = 55_000L
        assertEquals(12_000L, sessionClock.activeElapsedMillis())

        sessionClock.resume()
        clock.now = 56_000L
        assertEquals(13_000L, sessionClock.activeElapsedMillis())
    }
}
