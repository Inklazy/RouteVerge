package com.inklazy.routeverge.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.inklazy.routeverge.MainActivity
import com.inklazy.routeverge.data.LocationCache
import com.inklazy.routeverge.data.PlaybackMode
import com.inklazy.routeverge.data.RoutePoint
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.data.SimulatedLocation
import com.inklazy.routeverge.geo.RouteMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Persisted description/progress for the current simulation. It is not a restart signal. */
data class MockSessionSnapshot(
    val kind: String,
    val pointLat: Double? = null,
    val pointLng: Double? = null,
    val routeId: String? = null,
    val routeName: String? = null,
    val routeJson: String? = null,
    val speedMps: Double? = null,
    val closeLoop: Boolean = false,
    val loopCount: Int = 1,
    val activeElapsedMillis: Long = 0L,
    val isPaused: Boolean = false
)

class MockLocationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionLock = Any()
    private val sessionFence = MockSessionFence()
    private lateinit var locationManager: LocationManager
    private var job: Job? = null
    private var activeProviders: List<String> = emptyList()
    private val simulationClock = ActiveSimulationClock()
    private var lastComputedLocation: SimulatedLocation? = null
    private var consecutivePushFailures: Int = 0
    private var lastProgressWriteAtMillis: Long = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationText: String = "模拟定位中"

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        // A newly created service has no runtime session. Never infer one from
        // the persisted session store here.
        isRunning = false
        isPaused = false
        runtimeSession = null
        MockLocationStateStore.publish(false, false, null)
    }

    override fun onStartCommand(intent: Intent?, _flags: Int, startId: Int): Int {
        // This service is deliberately non-sticky. If Android kills the
        // process, the old intent must not be replayed and no persisted
        // session may be used to start mocking again.
        return when (intent?.action) {
            ACTION_START_POINT -> {
                startPoint(
                    lat = intent.getDoubleExtra(EXTRA_LAT, 0.0),
                    lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
                )
                START_NOT_STICKY
            }

            ACTION_START_ROUTE -> {
                startRoute(
                    routeJson = intent.getStringExtra(EXTRA_ROUTE_JSON).orEmpty(),
                    speedMps = intent.getDoubleExtra(EXTRA_SPEED_MPS, 0.0),
                    closeLoop = intent.getBooleanExtra(EXTRA_CLOSE_LOOP, false),
                    loopCount = intent.getIntExtra(EXTRA_LOOP_COUNT, 1).coerceAtLeast(1)
                )
                START_NOT_STICKY
            }

            ACTION_PAUSE -> {
                if (isRunning) pauseMocking() else stopSelfResult(startId)
                START_NOT_STICKY
            }

            ACTION_RESUME -> {
                if (isRunning) resumeMocking() else stopSelfResult(startId)
                START_NOT_STICKY
            }

            ACTION_STOP -> {
                stopMocking()
                START_NOT_STICKY
            }

            else -> {
                // A null/unknown command is not a recovery request. Do not
                // leave an inert started service behind.
                stopSelfResult(startId)
                START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        // stopWithTask=true stops this service when its task is removed, and
        // normal service teardown reaches this single cleanup path. Process
        // death can skip callbacks; the next cold start clears stale
        // persistence because it does not observe a live runtime.
        stopMocking(stopService = false)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPoint(lat: Double, lng: Double) {
        if (!lat.isFinite() || !lng.isFinite() || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            stopMocking()
            return
        }
        startMockLoop(
            notificationText = "定点模拟中：${formatCoord(lat)}, ${formatCoord(lng)}",
            writeSession = { writePointSession(lat, lng) },
            locationProvider = { _ ->
                SimulatedLocation(lat, lng, 0f, 0f, 5f, 10.0)
            }
        )
    }

    private fun startRoute(routeJson: String, speedMps: Double, closeLoop: Boolean, loopCount: Int) {
        val route = decodeRoute(routeJson)
        if (route == null || route.points.size < 2 ||
            route.points.any { !it.latWgs84.isFinite() || !it.lngWgs84.isFinite() ||
                it.latWgs84 !in -90.0..90.0 || it.lngWgs84 !in -180.0..180.0 } ||
            !speedMps.isFinite() || speedMps <= 0.0 || RouteMath.totalDistanceMeters(route.points) <= 0.0) {
            stopMocking()
            return
        }
        val playbackMode = if (closeLoop) PlaybackMode.LOOP else PlaybackMode.OUT_AND_BACK
        val maxElapsedMillis = if (closeLoop) {
            val loopDistance = RouteMath.totalDistanceMeters(route.points, closeLoop = true)
            ((loopDistance * loopCount / speedMps) * 1000.0).toLong()
        } else {
            Long.MAX_VALUE
        }

        startMockLoop(
            notificationText = "路线模拟中：${route.name} $speedMps m/s",
            writeSession = { writeRouteSession(route, routeJson, speedMps, closeLoop, loopCount) },
            locationProvider = { generation ->
                val elapsedMillis = activeElapsedMillis()
                if (elapsedMillis >= maxElapsedMillis) {
                    scope.launch { stopMocking(expectedGeneration = generation) }
                }
                RouteMath.interpolateRoute(route.points, elapsedMillis, speedMps, playbackMode)
            }
        )
    }

    private fun startMockLoop(
        notificationText: String,
        writeSession: () -> Unit,
        locationProvider: (Long) -> SimulatedLocation
    ) {
        if (!hasFineLocationPermission()) {
            stopMocking()
            return
        }

        // Provider ownership and startup are serialized with stop/recovery.
        stopMocking(stopService = false)
        val generation = synchronized(sessionLock) { sessionFence.nextGeneration() }
        try {
            synchronized(sessionLock) {
                if (!sessionFence.isCurrent(generation)) return
                this.notificationText = notificationText
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, buildNotification(notificationText),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
                val readyProviders = PROVIDERS.filter { setupProvider(it) }
                if (readyProviders.isEmpty()) error("No mock providers available")
                activeProviders = readyProviders
                acquireWakeLock()
                writeSession()
                val session = readSession(this) ?: error("Missing mock session")
                simulationClock.start()
                lastComputedLocation = locationProvider(generation)
                consecutivePushFailures = 0
                lastProgressWriteAtMillis = 0L
                runtimeSession = session
                isRunning = true
                isPaused = false
                MockLocationStateStore.publish(true, false, session)

                val initialLocation = lastComputedLocation ?: error("Missing initial location")
                var initialPushes = activeProviders.count { pushLocation(it, initialLocation) }
                if (initialPushes == 0) {
                    recoverProviders(generation)
                    initialPushes = activeProviders.count { pushLocation(it, initialLocation) }
                }
                if (initialPushes == 0) error("No mock providers accepted the initial location")

                job = scope.launch {
                    try {
                        while (isActive && isSessionActive(generation)) {
                            val computed = if (isPaused) lastComputedLocation ?: locationProvider(generation)
                                else locationProvider(generation)
                            val shouldStop = synchronized(sessionLock) {
                                if (!isSessionActive(generation)) return@launch
                                val loc = if (isPaused) lastComputedLocation ?: computed else computed.also {
                                    lastComputedLocation = it
                                }
                                val pushed = activeProviders.count { provider -> pushLocation(provider, loc) }
                                sessionFence.recordAccepted(generation, loc, pushed)
                                if (pushed == 0) {
                                    consecutivePushFailures += 1
                                    Log.w(TAG, "No mock providers accepted location; attempting recovery #$consecutivePushFailures")
                                    recoverProviders(generation)
                                } else {
                                    consecutivePushFailures = 0
                                }
                                val reachedLimit = pushed == 0 && consecutivePushFailures >= MAX_PROVIDER_RECOVERY_ATTEMPTS
                                if (!reachedLimit) {
                                    val wroteProgress = persistProgress(generation)
                                    if (pushed > 0 && wroteProgress) {
                                        sessionFence.acceptedLocation()?.let { accepted ->
                                            LocationCache.save(this@MockLocationService,
                                                RoutePoint(accepted.latWgs84, accepted.lngWgs84))
                                        }
                                    }
                                }
                                reachedLimit
                            }
                            if (shouldStop) {
                                Log.w(TAG, "Mock provider recovery failed too many times; stopping mock loop")
                                stopMocking(expectedGeneration = generation)
                                return@launch
                            }
                            delay(1000L)
                        }
                    } catch (e: Exception) {
                        if (isSessionActive(generation)) Log.e(TAG, "Mock loop failed", e)
                    } finally {
                        stopMocking(expectedGeneration = generation)
                    }
                }
                // The startup is now committed; a failed initial push never reaches this point.
                sessionFence.recordAccepted(generation, initialLocation, initialPushes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mock startup failed", e)
            stopMocking(expectedGeneration = generation)
        }
    }

    private fun isSessionActive(generation: Long): Boolean =
        isRunning && sessionFence.isCurrent(generation)

    private fun writePointSession(lat: Double, lng: Double) {
        sessionPreferences().edit().clear()
            .putBoolean(KEY_SESSION_PRESENT, true)
            .putString(KEY_KIND, KIND_POINT)
            .putString(KEY_POINT_LAT, lat.toString())
            .putString(KEY_POINT_LNG, lng.toString())
            .putLong(KEY_ELAPSED_MILLIS, 0L)
            .putBoolean(KEY_PAUSED, false)
            .apply()
    }

    private fun writeRouteSession(route: SavedRoute, routeJson: String, speedMps: Double, closeLoop: Boolean, loopCount: Int) {
        sessionPreferences().edit().clear()
            .putBoolean(KEY_SESSION_PRESENT, true)
            .putString(KEY_KIND, KIND_ROUTE)
            .putString(KEY_ROUTE_ID, route.id)
            .putString(KEY_ROUTE_NAME, route.name)
            .putString(KEY_ROUTE_JSON, routeJson)
            .putString(KEY_SPEED_MPS, speedMps.toString())
            .putBoolean(KEY_CLOSE_LOOP, closeLoop)
            .putInt(KEY_LOOP_COUNT, loopCount.coerceAtLeast(1))
            .putLong(KEY_ELAPSED_MILLIS, 0L)
            .putBoolean(KEY_PAUSED, false)
            .apply()
    }

    private fun persistProgress(generation: Long, force: Boolean = false): Boolean {
        synchronized(sessionLock) {
            if (!isSessionActive(generation)) return false
            val session = runtimeSession ?: return false
            val updatedSession = session.copy(
                activeElapsedMillis = activeElapsedMillis().coerceAtLeast(0L),
                isPaused = isPaused
            )
            runtimeSession = updatedSession
            MockLocationStateStore.publish(true, isPaused, updatedSession)
            val now = SystemClock.elapsedRealtime()
            if (!ProgressPersistence.shouldWrite(now, lastProgressWriteAtMillis, force)) return false
            sessionPreferences().edit()
                .putBoolean(KEY_SESSION_PRESENT, true)
                .putLong(KEY_ELAPSED_MILLIS, updatedSession.activeElapsedMillis)
                .putBoolean(KEY_PAUSED, updatedSession.isPaused)
                .apply()
            lastProgressWriteAtMillis = now
            return true
        }
    }

    private fun clearPersistedSession() {
        clearSessionPersistence(this)
    }

    private fun sessionPreferences() = getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    private fun refreshNotification(text: String) {
        if (!isRunning) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun pauseMocking() {
        val changed = synchronized(sessionLock) {
            if (!isRunning || isPaused) {
                false
            } else {
                simulationClock.pause()
                isPaused = true
                persistProgress(sessionFence.current(), force = true)
                sessionFence.acceptedLocation()?.let {
                    LocationCache.save(this, RoutePoint(it.latWgs84, it.lngWgs84))
                }
                true
            }
        }
        if (changed) refreshNotification("模拟已暂停")
    }

    private fun resumeMocking() {
        val changed = synchronized(sessionLock) {
            if (!isRunning || !isPaused) {
                false
            } else {
                simulationClock.resume()
                isPaused = false
                persistProgress(sessionFence.current(), force = true)
                true
            }
        }
        if (changed) refreshNotification(notificationText)
    }

    private fun stopMocking(stopService: Boolean = true, expectedGeneration: Long? = null) {
        synchronized(sessionLock) {
            // An old route-end task must not clear a newer session.
            if (!sessionFence.invalidate(expectedGeneration)) return
            val jobToCancel = job
            val finalAcceptedLocation = sessionFence.takeAccepted()
            job = null
            isRunning = false
            isPaused = false
            runtimeSession = null
            lastComputedLocation = null
            consecutivePushFailures = 0
            lastProgressWriteAtMillis = 0L
            runCatching { clearPersistedSession() }
                .onFailure { Log.w(TAG, "Failed to clear mock session", it) }
            MockLocationStateStore.publish(false, false, null)
            jobToCancel?.cancel()
            // Keep cleanup serialized with new registration and old recovery.
            removeActiveProviders()
            releaseWakeLock()
            finalAcceptedLocation?.let { location ->
                runCatching { LocationCache.save(this, RoutePoint(location.latWgs84, location.lngWgs84)) }
            }
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            runCatching { getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID) }
            if (stopService) stopSelf()
        }
    }

    private fun activeElapsedMillis(): Long = simulationClock.activeElapsedMillis()

    private fun hasFineLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun setupProvider(provider: String): Boolean {
        return runCatching {
            removeProvider(provider)
            MockPermission.addProvider(locationManager, provider)
            locationManager.setTestProviderEnabled(provider, true)
        }.onFailure {
            removeProvider(provider)
            Log.w(TAG, "Failed to set up mock provider $provider", it)
        }.isSuccess
    }

    private fun removeActiveProviders() {
        val providers = activeProviders.ifEmpty { PROVIDERS }
        providers.forEach(::removeProvider)
        activeProviders = emptyList()
    }

    private fun removeProvider(provider: String) {
        runCatching { locationManager.setTestProviderEnabled(provider, false) }
        runCatching { locationManager.removeTestProvider(provider) }
    }

    private fun recoverProviders(generation: Long) {
        synchronized(sessionLock) {
            if (!isSessionActive(generation)) return
            val recoveredProviders = buildList {
                PROVIDERS.forEach { provider ->
                    // stop/start cannot interleave with these provider operations.
                    if (!isSessionActive(generation)) return
                    if (setupProvider(provider)) add(provider)
                }
            }
            if (recoveredProviders.isEmpty()) return
            activeProviders = recoveredProviders
            lastComputedLocation?.let { location ->
                val pushed = activeProviders.count { provider -> pushLocation(provider, location) }
                if (job != null) sessionFence.recordAccepted(generation, location, pushed)
            }
        }
    }

    private fun pushLocation(provider: String, simulated: SimulatedLocation): Boolean {
        val location = Location(provider).apply {
            latitude = simulated.latWgs84
            longitude = simulated.lngWgs84
            accuracy = simulated.accuracyMeters
            altitude = simulated.altitudeMeters
            speed = simulated.speedMps
            bearing = simulated.bearing
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            extras = Bundle().apply {
                putInt("satellites", 8)
                putInt("satellitesvalue", 8)
            }
        }
        return runCatching {
            locationManager.setTestProviderLocation(provider, location)
        }.onFailure {
            Log.w(TAG, "Failed to push mock location to $provider", it)
        }.isSuccess
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:MockLocationKeepAlive"
        ).apply {
            setReferenceCounted(false)
        }
        wakeLock?.acquire()
    }

    private fun releaseWakeLock() {
        runCatching {
            wakeLock?.takeIf { it.isHeld }?.release()
        }
        wakeLock = null
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("RouteVerge")
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "模拟定位服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun decodeRoute(routeJson: String): SavedRoute? {
        return runCatching {
            val obj = JSONObject(routeJson)
            val pointsArray = obj.getJSONArray("points")
            val points = buildList {
                for (i in 0 until pointsArray.length()) {
                    val p = pointsArray.getJSONObject(i)
                    add(RoutePoint(p.getDouble("latWgs84"), p.getDouble("lngWgs84")))
                }
            }
            SavedRoute(
                id = obj.getString("id"),
                name = obj.getString("name"),
                points = points,
                closeLoop = obj.optBoolean("closeLoop", false),
                loopCount = obj.optInt("loopCount", 1).coerceAtLeast(1)
            )
        }.getOrNull()
    }

    private fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    companion object {
        const val ACTION_START_POINT = "com.inklazy.routeverge.START_POINT"
        const val ACTION_START_ROUTE = "com.inklazy.routeverge.START_ROUTE"
        const val ACTION_PAUSE = "com.inklazy.routeverge.PAUSE"
        const val ACTION_RESUME = "com.inklazy.routeverge.RESUME"
        const val ACTION_STOP = "com.inklazy.routeverge.STOP"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_ROUTE_JSON = "route_json"
        const val EXTRA_SPEED_MPS = "speed_mps"
        const val EXTRA_CLOSE_LOOP = "close_loop"
        const val EXTRA_LOOP_COUNT = "loop_count"

        private const val CHANNEL_ID = "mock_location"
        private const val NOTIFICATION_ID = 2201
        private const val TAG = "MockLocationService"
        private const val MAX_PROVIDER_RECOVERY_ATTEMPTS = 5
        private const val SESSION_PREFS = "mock_location_session"
        private const val KEY_SESSION_PRESENT = "session_present"
        private const val KEY_KIND = "kind"
        private const val KEY_POINT_LAT = "point_lat"
        private const val KEY_POINT_LNG = "point_lng"
        private const val KEY_ROUTE_ID = "route_id"
        private const val KEY_ROUTE_NAME = "route_name"
        private const val KEY_ROUTE_JSON = "route_json"
        private const val KEY_SPEED_MPS = "speed_mps"
        private const val KEY_CLOSE_LOOP = "close_loop"
        private const val KEY_LOOP_COUNT = "loop_count"
        private const val KEY_ELAPSED_MILLIS = "active_elapsed_ms"
        private const val KEY_PAUSED = "paused"
        private const val KIND_POINT = "point"
        private const val KIND_ROUTE = "route"
        private val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var isPaused: Boolean = false
            private set

        @Volatile
        private var runtimeSession: MockSessionSnapshot? = null

        fun currentSession(): MockSessionSnapshot? = runtimeSession.takeIf { isRunning }

        fun clearSessionPersistence(context: Context) {
            context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        fun readSession(context: Context): MockSessionSnapshot? {
            val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_SESSION_PRESENT, false)) return null
            val kind = prefs.getString(KEY_KIND, null) ?: return null
            return MockSessionSnapshot(
                kind = kind,
                pointLat = prefs.getString(KEY_POINT_LAT, null)?.toDoubleOrNull(),
                pointLng = prefs.getString(KEY_POINT_LNG, null)?.toDoubleOrNull(),
                routeId = prefs.getString(KEY_ROUTE_ID, null),
                routeName = prefs.getString(KEY_ROUTE_NAME, null),
                routeJson = prefs.getString(KEY_ROUTE_JSON, null),
                speedMps = prefs.getString(KEY_SPEED_MPS, null)?.toDoubleOrNull(),
                closeLoop = prefs.getBoolean(KEY_CLOSE_LOOP, false),
                loopCount = prefs.getInt(KEY_LOOP_COUNT, 1).coerceAtLeast(1),
                activeElapsedMillis = prefs.getLong(KEY_ELAPSED_MILLIS, 0L).coerceAtLeast(0L),
                isPaused = prefs.getBoolean(KEY_PAUSED, false)
            )
        }

        fun pointIntent(context: Context, lat: Double, lng: Double): Intent {
            return Intent(context, MockLocationService::class.java).apply {
                action = ACTION_START_POINT
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LNG, lng)
            }
        }

        fun routeIntent(
            context: Context,
            route: SavedRoute,
            speedMps: Double,
            closeLoop: Boolean,
            loopCount: Int
        ): Intent {
            return Intent(context, MockLocationService::class.java).apply {
                action = ACTION_START_ROUTE
                putExtra(EXTRA_SPEED_MPS, speedMps)
                putExtra(EXTRA_CLOSE_LOOP, closeLoop)
                putExtra(EXTRA_LOOP_COUNT, loopCount.coerceAtLeast(1))
                putExtra(EXTRA_ROUTE_JSON, JSONObject().apply {
                    put("id", route.id)
                    put("name", route.name)
                    put("closeLoop", route.closeLoop)
                    put("loopCount", route.loopCount)
                    put("points", JSONArray().apply {
                        route.points.forEach { point ->
                            put(JSONObject().apply {
                                put("latWgs84", point.latWgs84)
                                put("lngWgs84", point.lngWgs84)
                            })
                        }
                    })
                }.toString())
            }
        }

        fun pauseIntent(context: Context): Intent {
            return Intent(context, MockLocationService::class.java).apply {
                action = ACTION_PAUSE
            }
        }

        fun resumeIntent(context: Context): Intent {
            return Intent(context, MockLocationService::class.java).apply {
                action = ACTION_RESUME
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
