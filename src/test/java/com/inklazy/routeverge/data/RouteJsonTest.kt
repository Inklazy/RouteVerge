package com.inklazy.routeverge.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteJsonTest {
    @Test
    fun encodeDecodeRoutesRoundTrip() {
        val routes = listOf(
            SavedRoute(
                id = "route-1",
                name = "操场一圈",
                speedMps = 3.75,
                points = listOf(
                    RoutePoint(39.0, 116.0),
                    RoutePoint(39.001, 116.001)
                )
            )
        )

        val decoded = RouteJson.decode(RouteJson.encode(routes))

        assertEquals(routes, decoded)
    }

    @Test
    fun legacyRouteWithoutSpeedUsesDefault() {
        val legacy = """[{"id":"old","name":"旧路线","points":[{"latWgs84":0.0,"lngWgs84":0.0},{"latWgs84":0.0,"lngWgs84":0.001}]}]"""
        assertEquals(2.6, RouteJson.decode(legacy).single().speedMps, 0.0)
    }

    @Test
    fun invalidJsonReturnsEmptyList() {
        assertEquals(emptyList<SavedRoute>(), RouteJson.decode("{bad json"))
    }
}
