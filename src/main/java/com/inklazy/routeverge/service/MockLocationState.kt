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
 * collectors can come and go safely across configuration changes. The live
 * service runtime is the only source of truth for running/paused flags;
 * persistence is used only for session details and stale-session cleanup.
 */
object MockLocationStateStore {
    private val _state = MutableStateFlow(MockLocationState())
    val state: StateFlow<MockLocationState> = _state.asStateFlow()

    fun publish(isRunning: Boolean, isPaused: Boolean, session: MockSessionSnapshot?) {
        _state.value = MockLocationState(isRunning, isPaused, session)
    }

    fun syncFromRuntime(context: Context) {
        if (!MockLocationService.isRunning) {
            // This is the cold-start/process-death boundary: a persisted
            // session without a live service is stale and must not affect UI.
            MockLocationService.clearSessionPersistence(context)
            _state.value = MockLocationState()
            return
        }

        _state.value = runtimeState(
            serviceRunning = true,
            servicePaused = MockLocationService.isPaused,
            session = MockLocationService.currentSession()
        )
    }

    internal fun runtimeState(
        serviceRunning: Boolean,
        servicePaused: Boolean,
        session: MockSessionSnapshot?
    ): MockLocationState {
        if (!serviceRunning) return MockLocationState()
        return MockLocationState(
            isRunning = true,
            isPaused = servicePaused,
            session = session
        )
    }
}