package com.inklazy.routeverge.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Observable runtime state shared by the foreground service and the UI layer. */
data class MockLocationState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val session: MockSessionSnapshot? = null
)

/**
 * Process-local state bridge. It contains no Activity or Service reference, so
 * collectors can come and go safely across configuration changes. Persistence
 * remains the source of truth when the process is recreated.
 */
object MockLocationStateStore {
    private val _state = MutableStateFlow(MockLocationState())
    val state: StateFlow<MockLocationState> = _state.asStateFlow()

    fun publish(isRunning: Boolean, isPaused: Boolean, session: MockSessionSnapshot?) {
        _state.value = MockLocationState(isRunning, isPaused, session)
    }

    fun syncFromPersistence(context: Context) {
        val snapshot = MockLocationService.readSession(context)
        _state.value = MockLocationState(
            isRunning = snapshot != null || MockLocationService.isRunning,
            isPaused = MockLocationService.isPaused || snapshot?.isPaused == true,
            session = snapshot
        )
    }
}