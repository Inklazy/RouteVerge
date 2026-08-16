package com.example.campusrunner.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng as AMapLatLng
import com.amap.api.maps.model.MarkerOptions as AMapMarkerOptions
import com.amap.api.maps.model.PolylineOptions as AMapPolylineOptions
import com.example.campusrunner.BuildConfig
import com.example.campusrunner.data.MapProvider
import com.example.campusrunner.data.RoutePoint
import com.example.campusrunner.geo.CoordinateUtils
import com.example.campusrunner.ui.theme.RouteVergeBrand
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView as GoogleMapView
import com.google.android.gms.maps.CameraUpdateFactory as GoogleCameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory as GoogleBitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng as GoogleLatLng
import com.google.android.gms.maps.model.MarkerOptions as GoogleMarkerOptions
import com.google.android.gms.maps.model.PolylineOptions as GooglePolylineOptions
import java.util.Locale

enum class MarkerKind { START, END, CURRENT, NORMAL, CLOSED_ORIGIN }

interface MapMarkerHandle {
    fun setPosition(point: RoutePoint)
}

interface MapController {
    fun clear()
    fun addMarker(point: RoutePoint, title: String, kind: MarkerKind): MapMarkerHandle
    fun addPolyline(points: List<RoutePoint>, color: Int, width: Float)
    fun moveCamera(point: RoutePoint, zoom: Float? = null)
    fun animateCamera(point: RoutePoint, zoom: Float? = null)
    fun cameraTarget(): RoutePoint?
    fun screenPointToRoutePoint(x: Int, y: Int): RoutePoint?
    fun disableUiControls()
    fun setSatelliteEnabled(enabled: Boolean)
}

fun resolveMapProvider(selected: MapProvider): MapProvider {
    if (selected != MapProvider.AUTO) {
        return selected
    }
    return if (Locale.getDefault().country.equals("CN", ignoreCase = true)) {
        MapProvider.AMAP
    } else {
        MapProvider.GOOGLE
    }
}

@Composable
fun CampusMapView(
    provider: MapProvider,
    modifier: Modifier = Modifier,
    onReady: (MapController) -> Unit
) {
    when (resolveMapProvider(provider)) {
        MapProvider.AMAP -> AMapComposeView(modifier, onReady)
        MapProvider.GOOGLE -> GoogleComposeMapView(modifier, onReady)
        MapProvider.AUTO -> AMapComposeView(modifier, onReady)
    }
}

@Composable
private fun AMapComposeView(modifier: Modifier, onReady: (MapController) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapsInitializer.updatePrivacyShow(context.applicationContext, true, true)
        MapsInitializer.updatePrivacyAgree(context.applicationContext, true)
        MapsInitializer.setApiKey(BuildConfig.AMAP_API_KEY)
        TextureMapView(context).apply { onCreate(Bundle()) }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setOnTouchListener { view, _ ->
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                map.setOnMapLoadedListener {
                    onReady(AMapController(map))
                }
            }
        }
    )
}

@Composable
private fun GoogleComposeMapView(modifier: Modifier, onReady: (MapController) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        GoogleMapView(context).apply { onCreate(Bundle()) }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setOnTouchListener { view, _ ->
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                getMapAsync { map ->
                    onReady(GoogleController(map))
                }
            }
        }
    )
}

private class AMapController(private val map: AMap) : MapController {
    override fun clear() {
        map.clear()
    }

    override fun addMarker(point: RoutePoint, title: String, kind: MarkerKind): MapMarkerHandle {
        val gcj = CoordinateUtils.wgs84ToGcj02(point.latWgs84, point.lngWgs84)
        val marker = map.addMarker(
            AMapMarkerOptions()
                .position(AMapLatLng(gcj.lat, gcj.lng))
                .title(title)
                .icon(markerIcon(kind))
                .anchor(0.5f, 0.5f)
        )
        return object : MapMarkerHandle {
            override fun setPosition(point: RoutePoint) {
                val next = CoordinateUtils.wgs84ToGcj02(point.latWgs84, point.lngWgs84)
                marker?.position = AMapLatLng(next.lat, next.lng)
            }
        }
    }

    override fun addPolyline(points: List<RoutePoint>, color: Int, width: Float) {
        map.addPolyline(
            AMapPolylineOptions()
                .color(color)
                .width(width)
                .apply {
                    points.forEach { point ->
                        val gcj = CoordinateUtils.wgs84ToGcj02(point.latWgs84, point.lngWgs84)
                        add(AMapLatLng(gcj.lat, gcj.lng))
                    }
                }
        )
    }

    override fun moveCamera(point: RoutePoint, zoom: Float?) {
        val gcj = CoordinateUtils.wgs84ToGcj02(point.latWgs84, point.lngWgs84)
        if (zoom == null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(AMapLatLng(gcj.lat, gcj.lng)))
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(AMapLatLng(gcj.lat, gcj.lng), zoom))
        }
    }

    override fun animateCamera(point: RoutePoint, zoom: Float?) {
        val gcj = CoordinateUtils.wgs84ToGcj02(point.latWgs84, point.lngWgs84)
        if (zoom == null) {
            map.animateCamera(CameraUpdateFactory.newLatLng(AMapLatLng(gcj.lat, gcj.lng)))
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(AMapLatLng(gcj.lat, gcj.lng), zoom))
        }
    }

    override fun cameraTarget(): RoutePoint? {
        val target = map.cameraPosition?.target ?: return null
        val wgs = CoordinateUtils.gcj02ToWgs84(target.latitude, target.longitude)
        return RoutePoint(wgs.lat, wgs.lng)
    }

    override fun screenPointToRoutePoint(x: Int, y: Int): RoutePoint? {
        val target = map.projection?.fromScreenLocation(Point(x, y)) ?: return null
        val wgs = CoordinateUtils.gcj02ToWgs84(target.latitude, target.longitude)
        return RoutePoint(wgs.lat, wgs.lng)
    }

    override fun disableUiControls() {
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.setAllGesturesEnabled(true)
    }

    override fun setSatelliteEnabled(enabled: Boolean) {
        map.mapType = if (enabled) AMap.MAP_TYPE_SATELLITE else AMap.MAP_TYPE_NORMAL
    }

    private fun markerIcon(kind: MarkerKind) =
        BitmapDescriptorFactory.fromBitmap(createDotBitmap(markerDotColor(kind)))
}

private class GoogleController(private val map: GoogleMap) : MapController {
    override fun clear() {
        map.clear()
    }

    override fun addMarker(point: RoutePoint, title: String, kind: MarkerKind): MapMarkerHandle {
        val marker = map.addMarker(
            GoogleMarkerOptions()
                .position(GoogleLatLng(point.latWgs84, point.lngWgs84))
                .title(title)
                .icon(GoogleBitmapDescriptorFactory.fromBitmap(createDotBitmap(markerDotColor(kind))))
                .anchor(0.5f, 0.5f)
        )
        return object : MapMarkerHandle {
            override fun setPosition(point: RoutePoint) {
                marker?.position = GoogleLatLng(point.latWgs84, point.lngWgs84)
            }
        }
    }

    override fun addPolyline(points: List<RoutePoint>, color: Int, width: Float) {
        map.addPolyline(
            GooglePolylineOptions()
                .color(color)
                .width(width)
                .apply {
                    points.forEach { point -> add(GoogleLatLng(point.latWgs84, point.lngWgs84)) }
                }
        )
    }

    override fun moveCamera(point: RoutePoint, zoom: Float?) {
        if (zoom == null) {
            map.moveCamera(GoogleCameraUpdateFactory.newLatLng(GoogleLatLng(point.latWgs84, point.lngWgs84)))
        } else {
            map.moveCamera(GoogleCameraUpdateFactory.newLatLngZoom(GoogleLatLng(point.latWgs84, point.lngWgs84), zoom))
        }
    }

    override fun animateCamera(point: RoutePoint, zoom: Float?) {
        if (zoom == null) {
            map.animateCamera(GoogleCameraUpdateFactory.newLatLng(GoogleLatLng(point.latWgs84, point.lngWgs84)))
        } else {
            map.animateCamera(GoogleCameraUpdateFactory.newLatLngZoom(GoogleLatLng(point.latWgs84, point.lngWgs84), zoom))
        }
    }

    override fun cameraTarget(): RoutePoint? {
        val target = map.cameraPosition.target
        return RoutePoint(target.latitude, target.longitude)
    }

    override fun screenPointToRoutePoint(x: Int, y: Int): RoutePoint? {
        val target = map.projection.fromScreenLocation(Point(x, y))
        return RoutePoint(target.latitude, target.longitude)
    }

    override fun disableUiControls() {
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.setAllGesturesEnabled(true)
    }

    override fun setSatelliteEnabled(enabled: Boolean) {
        map.mapType = if (enabled) GoogleMap.MAP_TYPE_SATELLITE else GoogleMap.MAP_TYPE_NORMAL
    }
}

// ===== Shared compact marker rendering (cross-provider consistent) =====
// RouteVerge own marker semantics: small filled dots instead of SDK default pins.
private fun markerDotColor(kind: MarkerKind): Int = when (kind) {
    MarkerKind.START, MarkerKind.CLOSED_ORIGIN ->
        android.graphics.Color.parseColor(RouteVergeBrand.SuccessHex)
    MarkerKind.END -> android.graphics.Color.parseColor(RouteVergeBrand.DangerHex)
    MarkerKind.CURRENT, MarkerKind.NORMAL ->
        android.graphics.Color.parseColor(RouteVergeBrand.PrimaryHex)
}

private fun createDotBitmap(color: Int): Bitmap {
    val size = dotSizePx()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val radius = size / 2f
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(center, center, radius, border)
    canvas.drawCircle(center, center, radius * 0.7f, dot)
    return bitmap
}

private fun dotSizePx(): Int {
    // ~24dp compact marker, density-aware for cross-device consistency.
    val density = android.content.res.Resources.getSystem().displayMetrics.density
    return (24 * density).toInt()
}