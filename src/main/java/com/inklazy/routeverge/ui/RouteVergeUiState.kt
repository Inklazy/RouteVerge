package com.inklazy.routeverge.ui

import com.inklazy.routeverge.data.MapProvider
import com.inklazy.routeverge.data.SavedPoint
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.service.MockLocationState

/** All app-owned state consumed by the Compose tree. */
data class RouteVergeUiState(
    val hasLocationPermission: Boolean = false,
    val canMockLocation: Boolean = false,
    val routes: List<SavedRoute> = emptyList(),
    val savedPoints: List<SavedPoint> = emptyList(),
    val mapProvider: MapProvider = MapProvider.AUTO,
    val mockLocation: MockLocationState = MockLocationState(),
    val runtimeSession: RuntimeSession? = null
) {
    val isServiceRunning: Boolean get() = mockLocation.isRunning
    val isServicePaused: Boolean get() = mockLocation.isPaused
}