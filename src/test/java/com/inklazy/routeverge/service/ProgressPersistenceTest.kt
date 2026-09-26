package com.inklazy.routeverge.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPersistenceTest {
    @Test fun regularTicksAreThrottledButBoundaryWrites() {
        assertTrue(ProgressPersistence.shouldWrite(10_000L, 0L))
        assertFalse(ProgressPersistence.shouldWrite(14_999L, 10_000L))
        assertTrue(ProgressPersistence.shouldWrite(15_000L, 10_000L))
    }

    @Test fun pauseAndResumeForceAnImmediateWrite() {
        assertTrue(ProgressPersistence.shouldWrite(10_001L, 10_000L, force = true))
        assertTrue(ProgressPersistence.shouldWrite(500L, 10_000L))
    }
}
