package com.inklazy.routeverge.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLocationStateStoreTest {
    @Test
    fun serviceTransitionsAreRepresentedAsObservableState() {
        val session = MockSessionSnapshot(kind = "route", activeElapsedMillis = 250L)

        MockLocationStateStore.publish(isRunning = true, isPaused = false, session = session)
        assertTrue(MockLocationStateStore.state.value.isRunning)
        assertFalse(MockLocationStateStore.state.value.isPaused)
        assertTrue(MockLocationStateStore.state.value.session === session)

        MockLocationStateStore.publish(isRunning = true, isPaused = true, session = session.copy(isPaused = true))
        assertTrue(MockLocationStateStore.state.value.isRunning)
        assertTrue(MockLocationStateStore.state.value.isPaused)

        MockLocationStateStore.publish(isRunning = false, isPaused = false, session = null)
        assertFalse(MockLocationStateStore.state.value.isRunning)
        assertFalse(MockLocationStateStore.state.value.isPaused)
    }
}