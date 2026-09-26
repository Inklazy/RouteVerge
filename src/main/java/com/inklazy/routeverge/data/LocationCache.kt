package com.inklazy.routeverge.data

import android.content.Context
import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble

object LocationCache {
    private const val PREFS = "campus_runner_location_cache"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"
    private const val KEY_TIME = "time"
    internal const val MAX_AGE_MILLIS = 24L * 60 * 60 * 1000

    internal fun isFresh(savedAt: Long, now: Long): Boolean =
        savedAt > 0L && savedAt <= now && now - savedAt <= MAX_AGE_MILLIS

    fun save(context: Context, point: RoutePoint) {
        if (!point.isValid()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAT, doubleToRawLongBits(point.latWgs84))
            .putLong(KEY_LNG, doubleToRawLongBits(point.lngWgs84))
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun read(context: Context): RoutePoint? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LNG) || !prefs.contains(KEY_TIME)) {
            return null
        }
        val savedAt = runCatching { prefs.getLong(KEY_TIME, 0L) }.getOrNull() ?: return null
        if (!isFresh(savedAt, System.currentTimeMillis())) return null
        val point = runCatching {
            RoutePoint(
                latWgs84 = longBitsToDouble(prefs.getLong(KEY_LAT, doubleToRawLongBits(0.0))),
                lngWgs84 = longBitsToDouble(prefs.getLong(KEY_LNG, doubleToRawLongBits(0.0)))
            )
        }.getOrNull() ?: return null
        return point.takeIf { it.isValid() }
    }

    private fun RoutePoint.isValid(): Boolean {
        return latWgs84 in -90.0..90.0 && lngWgs84 in -180.0..180.0
    }
}
