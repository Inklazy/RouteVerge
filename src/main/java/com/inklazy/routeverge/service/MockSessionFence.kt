package com.inklazy.routeverge.service

import com.inklazy.routeverge.data.SimulatedLocation
import java.util.concurrent.atomic.AtomicLong

/** Session identity and the last location actually accepted by a mock provider.
 * Mutations are serialized by MockLocationService.sessionLock.
 */
internal class MockSessionFence {
    private val generation = AtomicLong(0L)
    private var lastAccepted: SimulatedLocation? = null

    fun current(): Long = generation.get()
    fun isCurrent(expected: Long): Boolean = generation.get() == expected
    fun nextGeneration(): Long = generation.incrementAndGet()

    fun invalidate(expected: Long? = null): Boolean {
        if (expected != null) return generation.compareAndSet(expected, expected + 1)
        generation.incrementAndGet()
        return true
    }

    fun recordAccepted(expected: Long, location: SimulatedLocation, acceptedCount: Int) {
        if (acceptedCount > 0 && isCurrent(expected)) lastAccepted = location
    }

    fun acceptedLocation(): SimulatedLocation? = lastAccepted

    fun takeAccepted(): SimulatedLocation? = lastAccepted.also { lastAccepted = null }
}
