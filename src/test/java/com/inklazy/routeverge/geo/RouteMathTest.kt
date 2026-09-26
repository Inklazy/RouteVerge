package com.inklazy.routeverge.geo

import com.inklazy.routeverge.data.PlaybackMode
import com.inklazy.routeverge.data.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMathTest {
    @Test
    fun totalDistanceSumsSegments() {
        val points = listOf(
            RoutePoint(39.0, 116.0),
            RoutePoint(39.0, 116.001),
            RoutePoint(39.001, 116.001)
        )

        val distance = RouteMath.totalDistanceMeters(points)

        assertTrue(distance in 195.0..205.0)
    }

    @Test
    fun loopPlaybackWrapsAroundToBeginning() {
        val points = listOf(
            RoutePoint(0.0, 0.0),
            RoutePoint(0.0, 0.001),
            RoutePoint(0.001, 0.001),
            RoutePoint(0.0, 0.0)
        )
        val loopDistance = RouteMath.totalDistanceMeters(points, closeLoop = true)

        val loc = RouteMath.interpolateRoute(
            points = points,
            elapsedMillis = ((loopDistance + 10.0) * 1000).toLong(),
            speedMps = 1.0,
            playbackMode = PlaybackMode.LOOP
        )

        val distanceFromStart = RouteMath.distanceMeters(RoutePoint(0.0, 0.0), RoutePoint(loc.latWgs84, loc.lngWgs84))
        assertTrue(distanceFromStart < 15.0)
    }

    @Test
    fun outAndBackPlaybackReturnsTowardStart() {
        val points = listOf(
            RoutePoint(0.0, 0.0),
            RoutePoint(0.0, 0.001)
        )
        val oneWayDistance = RouteMath.totalDistanceMeters(points)

        val loc = RouteMath.interpolateRoute(
            points = points,
            elapsedMillis = ((oneWayDistance + 20.0) * 1000).toLong(),
            speedMps = 1.0,
            playbackMode = PlaybackMode.OUT_AND_BACK
        )

        val distanceFromEnd = RouteMath.distanceMeters(points.last(), RoutePoint(loc.latWgs84, loc.lngWgs84))
        assertEquals(20.0, distanceFromEnd, 1.5)
        assertEquals(270.0, loc.bearing.toDouble(), 1.0)
    }

    @Test
    fun outAndBackBearingChangesAtTurnaroundWithoutMovingPositionForward() {
        val points = listOf(RoutePoint(0.0, 0.0), RoutePoint(0.0, 0.001))
        val oneWay = RouteMath.totalDistanceMeters(points)
        fun at(distance: Double) = RouteMath.interpolateRoute(
            points, (distance * 1000).toLong(), 1.0, PlaybackMode.OUT_AND_BACK
        )
        val outward = at(oneWay - 5)
        // Exactly one second at one-way-distance m/s lands on the endpoint.
        val endpoint = RouteMath.interpolateRoute(points, 1000L, oneWay, PlaybackMode.OUT_AND_BACK)
        val justReturning = RouteMath.interpolateRoute(points, 1001L, oneWay, PlaybackMode.OUT_AND_BACK)
        val returning = at(oneWay + 5)
        assertEquals(90.0, outward.bearing.toDouble(), 1.0)
        assertEquals(90.0, endpoint.bearing.toDouble(), 1.0)
        assertEquals(270.0, justReturning.bearing.toDouble(), 1.0)
        assertTrue(justReturning.lngWgs84 < endpoint.lngWgs84)
        assertEquals(270.0, returning.bearing.toDouble(), 1.0)
        assertEquals(points.last().lngWgs84, endpoint.lngWgs84, 0.000001)
        assertTrue(returning.lngWgs84 < endpoint.lngWgs84)
        assertEquals(outward.lngWgs84, returning.lngWgs84, 0.00001)
    }

    @Test
    fun multiSegmentReturnReportsTheReverseOfTheCurrentSegment() {
        val points = listOf(RoutePoint(0.0, 0.0), RoutePoint(0.0, 0.001), RoutePoint(0.001, 0.001))
        val oneWay = RouteMath.totalDistanceMeters(points)
        val loc = RouteMath.interpolateRoute(
            points, ((oneWay + 10.0) * 1000).toLong(), 1.0, PlaybackMode.OUT_AND_BACK
        )
        assertEquals(180.0, loc.bearing.toDouble(), 1.0)
        assertTrue(loc.latWgs84 < points.last().latWgs84)
        assertEquals(points.last().lngWgs84, loc.lngWgs84, 0.00001)
    }

    @Test
    fun loopClosingSegmentUsesClosingDirection() {
        val points = listOf(RoutePoint(0.0, 0.0), RoutePoint(0.0, 0.001), RoutePoint(0.001, 0.001))
        val forward = RouteMath.totalDistanceMeters(points)
        val closing = RouteMath.interpolateRoute(
            points, ((forward + 10.0) * 1000).toLong(), 1.0, PlaybackMode.LOOP
        )
        val expected = RouteMath.bearingDegrees(points.last(), points.first())
        assertEquals(expected, closing.bearing.toDouble(), 1.0)
        assertTrue(closing.lngWgs84 < points.last().lngWgs84)
    }
}
