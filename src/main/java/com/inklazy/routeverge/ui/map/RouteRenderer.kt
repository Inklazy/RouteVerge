package com.inklazy.routeverge.ui.map

import com.inklazy.routeverge.data.RoutePoint
import com.inklazy.routeverge.ui.theme.RouteVergeBrand
import com.inklazy.routeverge.ui.theme.RouteVergeMapTokens

/**
 * Renders a route (polyline + markers) onto a [MapController].
 *
 * Stored route points are decoupled from displayed markers:
 * the full point list drives the polyline, but only semantic markers are shown
 * (start / end / closed-loop origin / drawing current point) — never one
 * marker per sampled point.
 */
internal fun renderRoute(
    controller: MapController,
    points: List<RoutePoint>,
    closeLoopPreview: Boolean = false,
    previewPoints: List<RoutePoint> = emptyList(),
    drawMode: Boolean = false
) {
    controller.clear()

    // Committed route line — a coral route overlay; the underlying map is untouched.
    if (points.size >= 2) {
        val visiblePoints = if (closeLoopPreview) points + points.first() else points
        controller.addPolyline(visiblePoints, android.graphics.Color.parseColor(RouteVergeBrand.PrimaryHex), RouteVergeMapTokens.routeStrokeWidth)
    }

    // Semantic markers only — never a marker per sampled point.
    when {
        points.isEmpty() -> Unit
        closeLoopPreview -> {
            // Closed loop: a single origin marker (start and end coincide).
            controller.addMarker(points.first(), "", MarkerKind.CLOSED_ORIGIN)
        }

        drawMode -> {
            controller.addMarker(points.first(), "", MarkerKind.START)
            if (points.size >= 2) {
                controller.addMarker(points.last(), "", MarkerKind.CURRENT)
            }
        }

        else -> {
            controller.addMarker(points.first(), "", MarkerKind.START)
            if (points.size >= 2) {
                controller.addMarker(points.last(), "", MarkerKind.END)
            }
        }
    }

    // Template preview — deliberately lighter than the committed route:
    // success green at reduced alpha and a thinner stroke (dash is not
    // consistent across AMap / Google, so unified opacity wins).
    if (previewPoints.size >= 2) {
        val previewColor =
            (android.graphics.Color.parseColor(RouteVergeBrand.SuccessHex) and 0x00FFFFFF) or (RouteVergeMapTokens.secondaryRouteAlpha shl 24)
        controller.addPolyline(previewPoints + previewPoints.first(), previewColor, RouteVergeMapTokens.previewRouteStrokeWidth)
    }
}
