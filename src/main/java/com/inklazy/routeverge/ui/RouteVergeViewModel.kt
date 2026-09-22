package com.inklazy.routeverge.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inklazy.routeverge.data.LocationCache
import com.inklazy.routeverge.data.MapProvider
import com.inklazy.routeverge.data.PointRepository
import com.inklazy.routeverge.data.RoutePoint
import com.inklazy.routeverge.data.RouteRepository
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.data.UserSettings
import com.inklazy.routeverge.geo.RouteMath
import com.inklazy.routeverge.service.MockLocationService
import com.inklazy.routeverge.service.MockLocationStateStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RouteVergeEvent {
    data class ShowMessage(val message: String) : RouteVergeEvent
    data object RequestLocationPermissions : RouteVergeEvent
    data object OpenDeveloperOptions : RouteVergeEvent
}

/**
 * Application state and user actions for the main RouteVerge flow.
 * Android-specific entry points (permission launcher, NFC, external intents)
 * remain in MainActivity; data, service commands and CRUD live here.
 */
class RouteVergeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val routeRepository = RouteRepository(appContext)
    private val pointRepository = PointRepository(appContext)
    private val userSettings = UserSettings(appContext)

    private val _uiState = MutableStateFlow(RouteVergeUiState())
    val uiState: StateFlow<RouteVergeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RouteVergeEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<RouteVergeEvent> = _events.asSharedFlow()

    init {
        refresh()
        viewModelScope.launch {
            MockLocationStateStore.state.collectLatest { serviceState ->
                _uiState.update { current ->
                    current.copy(
                        mockLocation = serviceState,
                        runtimeSession = runtimeSessionFrom(serviceState.session, current.routes)
                    )
                }
            }
        }
    }

    fun refresh() {
        MockLocationStateStore.syncFromPersistence(appContext)
        _uiState.update {
            it.copy(
                hasLocationPermission = hasFineLocationPermission(),
                canMockLocation = hasFineLocationPermission() && MockPermissionFacade.canUse(appContext),
                routes = routeRepository.getRoutes(),
                savedPoints = pointRepository.getPoints(),
                mapProvider = userSettings.mapProvider
            )
        }
        val serviceState = MockLocationStateStore.state.value
        _uiState.update { it.copy(runtimeSession = runtimeSessionFrom(serviceState.session, it.routes)) }
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(
                hasLocationPermission = hasFineLocationPermission(),
                canMockLocation = hasFineLocationPermission() && MockPermissionFacade.canUse(appContext)
            )
        }
        MockLocationStateStore.syncFromPersistence(appContext)
    }

    fun setMapProvider(provider: MapProvider) {
        userSettings.mapProvider = provider
        _uiState.update { it.copy(mapProvider = provider) }
    }

    fun startPoint(latText: String, lngText: String) {
        val lat = latText.toDoubleOrNull()
        val lng = lngText.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            emitMessage("请输入有效经纬度")
            return
        }
        startServiceSafely(MockLocationService.pointIntent(appContext, lat, lng))
    }

    fun startRoute(route: SavedRoute, speedText: String) {
        if (route.points.size < 2) {
            emitMessage("路线至少需要 2 个点")
            return
        }
        val speed = speedText.toDoubleOrNull()
        if (speed == null || speed <= 0.0) {
            emitMessage("速度必须大于 0")
            return
        }
        startServiceSafely(
            MockLocationService.routeIntent(
                context = appContext,
                route = route,
                speedMps = speed,
                closeLoop = route.closeLoop,
                loopCount = route.loopCount
            )
        )
    }

    fun pauseMocking() {
        if (!_uiState.value.isServiceRunning || _uiState.value.isServicePaused) return
        appContext.startService(MockLocationService.pauseIntent(appContext))
    }

    fun resumeMocking() {
        if (!_uiState.value.isServiceRunning || !_uiState.value.isServicePaused) return
        appContext.startService(MockLocationService.resumeIntent(appContext))
    }

    fun stopMocking() {
        if (!_uiState.value.isServiceRunning && !_uiState.value.isServicePaused) return
        appContext.startService(MockLocationService.stopIntent(appContext))
    }

    fun deleteRoute(route: SavedRoute) {
        routeRepository.deleteRoute(route.id)
        reloadRecords()
    }

    fun saveRoute(name: String, points: List<RoutePoint>, closeLoop: Boolean, loopCount: Int): SavedRoute? {
        if (name.isBlank()) {
            emitMessage("请输入路线名称")
            return null
        }
        if (points.size < 2) {
            emitMessage("路线至少需要 2 个点")
            return null
        }
        val route = routeRepository.saveRoute(name, points, closeLoop, loopCount)
        reloadRecords()
        emitMessage("路线已保存")
        return route
    }

    fun savePoint(name: String?, latText: String, lngText: String): Boolean {
        val point = parsePoint(latText, lngText) ?: return false
        pointRepository.savePoint(name, point)
        reloadRecords()
        emitMessage("点位已保存")
        return true
    }

    fun updatePoint(id: String, name: String?, latText: String, lngText: String): Boolean {
        val point = parsePoint(latText, lngText) ?: return false
        if (pointRepository.updatePoint(id, name, point) == null) return false
        reloadRecords()
        emitMessage("点位已更新")
        return true
    }

    fun deletePoint(id: String) {
        if (pointRepository.deletePoint(id)) reloadRecords()
    }

    @SuppressLint("MissingPermission")
    fun lastKnownRoutePoint(): RoutePoint? {
        if (!hasFineLocationPermission()) {
            _events.tryEmit(RouteVergeEvent.RequestLocationPermissions)
            return null
        }
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providerNames = runCatching { locationManager.allProviders }.getOrDefault(
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        )
        val location = providerNames
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            .maxByOrNull(Location::getTime)
        if (location != null) {
            val point = RoutePoint(location.latitude, location.longitude)
            LocationCache.save(appContext, point)
            return point
        }
        LocationCache.read(appContext)?.let {
            emitMessage("使用上次记录的位置定位地图")
            return it
        }
        emitMessage("暂时没有当前位置，请先打开系统定位或稍后再试")
        return null
    }

    private fun startServiceSafely(intent: Intent) {
        val state = _uiState.value
        if (state.isServiceRunning || state.isServicePaused) return
        if (!state.hasLocationPermission) {
            _events.tryEmit(RouteVergeEvent.RequestLocationPermissions)
            return
        }
        if (!state.canMockLocation) {
            _events.tryEmit(RouteVergeEvent.ShowMessage("请先在开发者选项中选择本应用为模拟位置应用"))
            _events.tryEmit(RouteVergeEvent.OpenDeveloperOptions)
            return
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } catch (_: SecurityException) {
            _events.tryEmit(RouteVergeEvent.ShowMessage("没有模拟位置权限，请重新选择模拟位置应用"))
            _events.tryEmit(RouteVergeEvent.OpenDeveloperOptions)
        }
    }

    private fun reloadRecords() {
        _uiState.update {
            it.copy(
                routes = routeRepository.getRoutes(),
                savedPoints = pointRepository.getPoints()
            )
        }
    }

    private fun parsePoint(latText: String, lngText: String): RoutePoint? {
        val lat = latText.toDoubleOrNull()
        val lng = lngText.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            emitMessage("请输入有效经纬度")
            return null
        }
        return RoutePoint(lat, lng)
    }

    private fun runtimeSessionFrom(snapshot: com.inklazy.routeverge.service.MockSessionSnapshot?, routes: List<SavedRoute>): RuntimeSession? {
        return when (snapshot?.kind) {
            "point" -> RuntimeSession(RuntimeSessionKind.POINT, pointLat = snapshot.pointLat, pointLng = snapshot.pointLng)
            "route" -> RuntimeSession(
                kind = RuntimeSessionKind.ROUTE,
                routeId = snapshot.routeId,
                routeName = snapshot.routeName,
                speedMps = snapshot.speedMps,
                closeLoop = snapshot.closeLoop,
                loopCount = snapshot.loopCount,
                totalDistanceMeters = routes.firstOrNull { it.id == snapshot.routeId }
                    ?.let { RouteMath.totalDistanceMeters(it.points, closeLoop = it.closeLoop) },
                activeElapsedMillis = snapshot.activeElapsedMillis
            )
            else -> null
        }
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun emitMessage(message: String) {
        _events.tryEmit(RouteVergeEvent.ShowMessage(message))
    }
}

private object MockPermissionFacade {
    fun canUse(context: Context): Boolean = com.inklazy.routeverge.service.MockPermission.canUseMockLocation(context)
}