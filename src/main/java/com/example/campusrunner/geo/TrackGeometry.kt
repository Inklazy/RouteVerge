package com.example.campusrunner.geo

import com.example.campusrunner.data.RoutePoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure geometry helpers for track templates (stadium-shaped oval).
 * Business-only, no UI dependencies.
 */
fun buildTrackOval(
    center: RoutePoint,
    lengthMeters: Double,
    widthMeters: Double,
    rotationDegrees: Double,
    samples: Int = 80
): List<RoutePoint> {
    val safeWidth = widthMeters.coerceIn(20.0, 160.0)
    val safeLength = lengthMeters.coerceAtLeast(safeWidth + 10.0).coerceAtMost(400.0)
    val radius = safeWidth / 2.0
    val halfStraight = (safeLength - safeWidth) / 2.0
    val rotation = Math.toRadians(rotationDegrees)

    return List(samples) { index ->
        val t = 2.0 * PI * index / samples
        val baseX: Double
        val baseY: Double
        if (cos(t) >= 0.0) {
            baseX = halfStraight + radius * cos(t)
            baseY = radius * sin(t)
        } else {
            baseX = -halfStraight + radius * cos(t)
            baseY = radius * sin(t)
        }
        val eastMeters = baseX * cos(rotation) - baseY * sin(rotation)
        val northMeters = baseX * sin(rotation) + baseY * cos(rotation)
        offsetMeters(center, eastMeters, northMeters)
    }
}

private fun offsetMeters(origin: RoutePoint, eastMeters: Double, northMeters: Double): RoutePoint {
    val earthRadiusMeters = 6378137.0
    val dLat = northMeters / earthRadiusMeters
    val dLng = eastMeters / (earthRadiusMeters * cos(Math.toRadians(origin.latWgs84)).coerceAtLeast(0.000001))
    return RoutePoint(
        latWgs84 = origin.latWgs84 + Math.toDegrees(dLat),
        lngWgs84 = origin.lngWgs84 + Math.toDegrees(dLng)
    )
}
