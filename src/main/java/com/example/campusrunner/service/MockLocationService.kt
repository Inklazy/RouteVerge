package com.example.campusrunner.service

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
import com.example.campusrunner.MainActivity
import com.example.campusrunner.data.LocationCache
import com.example.campusrunner.data.PlaybackMode
import com.example.campusrunner.data.RoutePoint
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.data.SimulatedLocation
import com.example.campusrunner.geo.RouteMath
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

/** Persisted description/progress used to restore a foreground simulation. */
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
    private lateinit var locationManager: LocationManager
    private var job: Job? = null
    private var activeProviders: List<String> = emptyList()
    private var startedAtMillis: Long = 0L
    private var pausedAtMillis: Long = 0L
    private var totalPausedMillis: Long = 0L
    private var lastPushedLocation: SimulatedLocation? = null
    private var consecutivePushFailures: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationText: String = "模拟定位中"

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_REDELIVER_INTENT may arrive after the process was reclaimed.
        // Restore the persisted clock/progress instead of starting at zero.
        if (intent == null) {
            return if (restorePersistedSession()) START_STICKY else START_NOT_STICKY
        }
        val restoreExisting = (flags and START_FLAG_REDELIVERY) != 0
        return when (intent.action) {
            ACTION_START_POINT -> {
                startPoint(
                lat = intent.getDoubleExtra(EXTRA_LAT, 0.0),
                lng = intent.getDoubleExtra(EXTRA_LNG, 0.0),
                restoreExisting = restoreExisting
                )
                START_REDELIVER_INTENT
            }

            ACTION_START_ROUTE -> {
                startRoute(
                    routeJson = intent.getStringExtra(EXTRA_ROUTE_JSON).orEmpty(),
                    speedMps = intent.getDoubleExtra(EXTRA_SPEED_MPS, 0.0),
                    closeLoop = intent.getBooleanExtra(EXTRA_CLOSE_LOOP, false),
                    loopCount = intent.getIntExtra(EXTRA_LOOP_COUNT, 1).coerceAtLeast(1),
                    restoreExisting = restoreExisting
                )
                START_REDELIVER_INTENT
            }

            ACTION_PAUSE -> {
                pauseMocking()
                START_STICKY
            }

            ACTION_RESUME -> {
                resumeMocking()
                START_STICKY
            }

            ACTION_STOP -> {
                stopMocking()
                START_NOT_STICKY
            }

            else -> if (isRunning || restorePersistedSession()) START_STICKY else START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        stopMocking(stopService = false)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPoint(lat: Double, lng: Double, restoreExisting: Boolean = false) {
        if (!restoreExisting || readSession(this) == null) {
            writePointSession(lat, lng)
        }
        startMockLoop(
            notificationText = "定点模拟中：${formatCoord(lat)}, ${formatCoord(lng)}",
            restoreExisting = restoreExisting,
            locationProvider = {
                SimulatedLocation(
                    latWgs84 = lat,
                    lngWgs84 = lng,
                    speedMps = 0f,
                    bearing = 0f,
                    accuracyMeters = 5f,
                    altitudeMeters = 10.0
                )
            }
        )
    }

    private fun startRoute(routeJson: String, speedMps: Double, closeLoop: Boolean, loopCount: Int, restoreExisting: Boolean = false) {
        val route = decodeRoute(routeJson) ?: return
        if (speedMps <= 0.0) {
            stopSelf()
            return
        }
        val playbackMode = if (closeLoop) PlaybackMode.LOOP else PlaybackMode.OUT_AND_BACK
        val maxElapsedMillis = if (closeLoop) {
            val loopDistance = RouteMath.totalDistanceMeters(route.points, closeLoop = true)
            ((loopDistance * loopCount / speedMps) * 1000.0).toLong()
        } else {
            Long.MAX_VALUE
        }

        if (!restoreExisting || readSession(this) == null) {
            writeRouteSession(route, routeJson, speedMps, closeLoop, loopCount)
        }
        startMockLoop(
            notificationText = "路线模拟中：${route.name} $speedMps m/s",
            restoreExisting = restoreExisting,
            locationProvider = {
                val elapsedMillis = activeElapsedMillis()
                if (elapsedMillis >= maxElapsedMillis) {
                    scope.launch { stopMocking() }
                }
                RouteMath.interpolateRoute(
                    points = route.points,
                    elapsedMillis = elapsedMillis,
                    speedMps = speedMps,
                    playbackMode = playbackMode
                )
            }
        )
    }

    private fun startMockLoop(notificationText: String, restoreExisting: Boolean = false, locationProvider: () -> SimulatedLocation) {
        if (!hasFineLocationPermission()) {
            clearPersistedSession()
            stopSelf()
            return
        }

        job?.cancel()
        job = null
        removeActiveProviders()

        val readyProviders = PROVIDERS.filter { setupProvider(it) }
        if (readyProviders.isEmpty()) {
            isRunning = false
            isPaused = false
            clearPersistedSession()
            removeActiveProviders()
            stopSelf()
            return
        }
        activeProviders = readyProviders

        acquireWakeLock()
        val persisted = if (restoreExisting) readSession(this) else null
        startedAtMillis = if (persisted != null) {
            System.currentTimeMillis() - persisted.activeElapsedMillis.coerceAtLeast(0L)
        } else {
            System.currentTimeMillis()
        }
        pausedAtMillis = if (persisted?.isPaused == true) System.currentTimeMillis() else 0L
        totalPausedMillis = 0L
        lastPushedLocation = locationProvider()
        consecutivePushFailures = 0
        isRunning = true
        isPaused = persisted?.isPaused == true
        this.notificationText = notificationText
        startForeground(
            NOTIFICATION_ID,
            buildNotification(if (isPaused) "模拟已暂停" else notificationText)
        )
        activeProviders.forEach { provider ->
            pushLocation(provider, lastPushedLocation ?: locationProvider())
        }

        job = scope.launch {
            while (isActive) {
                val loc = if (isPaused) {
                    lastPushedLocation ?: locationProvider()
                } else {
                    locationProvider().also { lastPushedLocation = it }
                }
                val pushed = activeProviders.count { provider -> pushLocation(provider, loc) }
                if (pushed == 0) {
                    consecutivePushFailures += 1
                    Log.w(TAG, "No mock providers accepted location; attempting recovery #$consecutivePushFailures")
                    recoverProviders()
                    if (consecutivePushFailures >= MAX_PROVIDER_RECOVERY_ATTEMPTS) {
                        Log.w(TAG, "Mock provider recovery failed too many times; stopping mock loop")
                        stopMocking()
                        return@launch
                    }
                } else {
                    consecutivePushFailures = 0
                    LocationCache.save(this@MockLocationService, RoutePoint(loc.latWgs84, loc.lngWgs84))
                }
                persistProgress()
                delay(1000L)
            }
        }
    }

    private fun writePointSession(lat: Double, lng: Double) {
        sessionPreferences().edit().clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_KIND, KIND_POINT)
            .putString(KEY_POINT_LAT, lat.toString())
            .putString(KEY_POINT_LNG, lng.toString())
            .putLong(KEY_ACTIVE_ELAPSED, 0L)
            .putBoolean(KEY_PAUSED, false)
            .apply()
    }

    private fun writeRouteSession(route: SavedRoute, routeJson: String, speedMps: Double, closeLoop: Boolean, loopCount: Int) {
        sessionPreferences().edit().clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_KIND, KIND_ROUTE)
            .putString(KEY_ROUTE_ID, route.id)
            .putString(KEY_ROUTE_NAME, route.name)
            .putString(KEY_ROUTE_JSON, routeJson)
            .putString(KEY_SPEED_MPS, speedMps.toString())
            .putBoolean(KEY_CLOSE_LOOP, closeLoop)
            .putInt(KEY_LOOP_COUNT, loopCount.coerceAtLeast(1))
            .putLong(KEY_ACTIVE_ELAPSED, 0L)
            .putBoolean(KEY_PAUSED, false)
            .apply()
    }

    private fun persistProgress() {
        if (!isRunning) return
        sessionPreferences().edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_ACTIVE_ELAPSED, activeElapsedMillis().coerceAtLeast(0L))
            .putBoolean(KEY_PAUSED, isPaused)
            .apply()
    }

    private fun clearPersistedSession() {
        sessionPreferences().edit().clear().apply()
    }

    private fun sessionPreferences() = getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    private fun restorePersistedSession(): Boolean {
        val snapshot = readSession(this) ?: return false
        val restored = when (snapshot.kind) {
            KIND_POINT -> {
                val lat = snapshot.pointLat
                val lng = snapshot.pointLng
                if (lat == null || lng == null) false else {
                    startPoint(lat, lng, restoreExisting = true)
                    true
                }
            }

            KIND_ROUTE -> {
                val json = snapshot.routeJson
                val speed = snapshot.speedMps
                if (json.isNullOrBlank() || speed == null) false else {
                    startRoute(json, speed, snapshot.closeLoop, snapshot.loopCount, restoreExisting = true)
                    true
                }
            }

            else -> false
        }
        if (!restored) clearPersistedSession()
        return restored
    }

    private fun refreshNotification(text: String) {
        if (!isRunning) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun pauseMocking() {
        if (!isRunning || isPaused) return
        pausedAtMillis = System.currentTimeMillis()
        isPaused = true
        persistProgress()
        refreshNotification("模拟已暂停")
    }

    private fun resumeMocking() {
        if (!isRunning || !isPaused) return
        totalPausedMillis += System.currentTimeMillis() - pausedAtMillis
        pausedAtMillis = 0L
        isPaused = false
        persistProgress()
        refreshNotification(notificationText)
    }

    private fun stopMocking(stopService: Boolean = true) {
        job?.cancel()
        job = null
        isRunning = false
        isPaused = false
        pausedAtMillis = 0L
        totalPausedMillis = 0L
        lastPushedLocation = null
        consecutivePushFailures = 0
        clearPersistedSession()
        removeActiveProviders()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (stopService) {
            stopSelf()
        }
    }

    private fun activeElapsedMillis(): Long {
        val currentPause = if (isPaused && pausedAtMillis > 0L) {
            System.currentTimeMillis() - pausedAtMillis
        } else {
            0L
        }
        return System.currentTimeMillis() - startedAtMillis - totalPausedMillis - currentPause
    }

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

    private fun recoverProviders() {
        val restoredProviders = PROVIDERS.filter { setupProvider(it) }
        if (restoredProviders.isNotEmpty()) {
            activeProviders = restoredProviders
            lastPushedLocation?.let { location ->
                activeProviders.forEach { provider -> pushLocation(provider, location) }
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
            acquire()
        }
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
        const val ACTION_START_POINT = "com.example.campusrunner.START_POINT"
        const val ACTION_START_ROUTE = "com.example.campusrunner.START_ROUTE"
        const val ACTION_PAUSE = "com.example.campusrunner.PAUSE"
        const val ACTION_RESUME = "com.example.campusrunner.RESUME"
        const val ACTION_STOP = "com.example.campusrunner.STOP"
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
        private const val KEY_ACTIVE = "active"
        private const val KEY_KIND = "kind"
        private const val KEY_POINT_LAT = "point_lat"
        private const val KEY_POINT_LNG = "point_lng"
        private const val KEY_ROUTE_ID = "route_id"
        private const val KEY_ROUTE_NAME = "route_name"
        private const val KEY_ROUTE_JSON = "route_json"
        private const val KEY_SPEED_MPS = "speed_mps"
        private const val KEY_CLOSE_LOOP = "close_loop"
        private const val KEY_LOOP_COUNT = "loop_count"
        private const val KEY_ACTIVE_ELAPSED = "active_elapsed_ms"
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

        fun readSession(context: Context): MockSessionSnapshot? {
            val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ACTIVE, false)) return null
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
                activeElapsedMillis = prefs.getLong(KEY_ACTIVE_ELAPSED, 0L).coerceAtLeast(0L),
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
