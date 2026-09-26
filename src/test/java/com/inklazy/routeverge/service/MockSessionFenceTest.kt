package com.inklazy.routeverge.service

import com.inklazy.routeverge.data.SimulatedLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSessionFenceTest {
    private val point = SimulatedLocation(39.0, 116.0, 0f, 0f, 5f, 10.0)

    @Test fun failedInitialPushAndRollbackDoNotSupplyACacheCandidate() {
        val fence = MockSessionFence()
        val generation = fence.nextGeneration()
        fence.recordAccepted(generation, point, acceptedCount = 0)
        assertTrue(fence.invalidate(generation))
        val cached = mutableListOf<SimulatedLocation>()
        fence.takeAccepted()?.let(cached::add) // same candidate consumed by stop cleanup
        assertTrue(cached.isEmpty())
    }

    @Test fun onlySuccessfulPushCanBeCached() {
        val fence = MockSessionFence()
        val generation = fence.nextGeneration()
        fence.recordAccepted(generation, point, acceptedCount = 1)
        assertEquals(point, fence.acceptedLocation())
        assertTrue(fence.invalidate(generation))
        assertEquals(point, fence.takeAccepted())
        assertNull(fence.takeAccepted())
    }

    @Test fun rejectedLaterPushDoesNotReplaceLastAcceptedPosition() {
        val fence = MockSessionFence()
        val generation = fence.nextGeneration()
        val rejected = point.copy(latWgs84 = 40.0)
        fence.recordAccepted(generation, point, acceptedCount = 1)
        fence.recordAccepted(generation, rejected, acceptedCount = 0)
        assertEquals(point, fence.takeAccepted())
    }

    @Test fun delayedStopFromSessionACannotInvalidateSessionB() {
        val fence = MockSessionFence()
        val sessionA = fence.nextGeneration()
        assertTrue(fence.invalidate(sessionA))
        val sessionB = fence.nextGeneration()
        assertFalse(fence.invalidate(sessionA))
        assertTrue(fence.isCurrent(sessionB))
        fence.recordAccepted(sessionA, point, acceptedCount = 1)
        assertNull(fence.acceptedLocation())
    }
}
