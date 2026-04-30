package com.example.productivityapp.ui.run

import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.example.productivityapp.run.RunMapViewportCalculator
import com.example.productivityapp.run.RunMapViewport
import com.example.productivityapp.run.RunViewportPoint
import com.example.productivityapp.ui.theme.RUN_ACCENT_HEX
import com.example.productivityapp.util.PolylineUtils
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

internal const val RUN_MAP_CONTENT_DESCRIPTION = "Run route map"

@Composable
fun RunMapView(
    polylineEncoded: String,
    modifier: Modifier = Modifier,
    replayPointIndex: Int? = null,
    replayViewport: RunMapViewport? = null,
    followRoute: Boolean = true,
    onMapReady: ((MapLibreMap) -> Unit)? = null,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val targetStyleUrl = remember(isDarkTheme) { RunMapStyleProvider.styleUrl(isDarkTheme) }
    val decodedPoints = remember(polylineEncoded) {
        if (polylineEncoded.isBlank()) emptyList() else PolylineUtils.decode(polylineEncoded)
    }
    val viewportCalculator = remember { RunMapViewportCalculator() }
    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            onCreate(null)
            onStart()
            onResume()
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.semantics {
            contentDescription = "$RUN_MAP_CONTENT_DESCRIPTION (${decodedPoints.size} points)"
        },
        update = { view ->
            view.getMapAsync { map ->
                val styleNeedsUpdate = view.tag != targetStyleUrl || map.style == null
                if (styleNeedsUpdate) {
                    view.tag = targetStyleUrl
                    map.setStyle(targetStyleUrl) {
                        configureRunMap(map)
                        onMapReady?.invoke(map)
                        applyRunRouteOverlays(
                            map = map,
                            decoded = decodedPoints,
                            replayPointIndex = replayPointIndex,
                            replayViewport = replayViewport,
                            followRoute = followRoute,
                            viewportCalculator = viewportCalculator,
                        )
                    }
                } else {
                    configureRunMap(map)
                    onMapReady?.invoke(map)
                    applyRunRouteOverlays(
                        map = map,
                        decoded = decodedPoints,
                        replayPointIndex = replayPointIndex,
                        replayViewport = replayViewport,
                        followRoute = followRoute,
                        viewportCalculator = viewportCalculator,
                    )
                }
            }
        },
    )
}

private fun configureRunMap(map: MapLibreMap) {
    map.uiSettings.apply {
        isCompassEnabled = false
        isTiltGesturesEnabled = false
        isRotateGesturesEnabled = false
    }
}

internal fun applyRunRouteOverlays(
    map: MapLibreMap,
    decoded: List<Pair<Double, Double>>,
    replayPointIndex: Int?,
    replayViewport: RunMapViewport?,
    followRoute: Boolean,
    viewportCalculator: RunMapViewportCalculator = RunMapViewportCalculator(),
) {
    map.clear()

    if (decoded.isEmpty()) {
        return
    }

    val visiblePairs = if (replayPointIndex != null && replayPointIndex >= 0) {
        decoded.take((replayPointIndex + 1).coerceAtMost(decoded.size))
    } else {
        decoded
    }
    val points = visiblePairs.map { (lat, lon) -> LatLng(lat, lon) }
    if (points.isEmpty()) return

    map.addPolyline(
        PolylineOptions()
            .addAll(points)
            .color(RUN_ACCENT_HEX.toColorInt() and 0x55FFFFFF)
            .alpha(0.35f)
            .width(14f)
    )
    map.addPolyline(
        PolylineOptions()
            .addAll(points)
            .color(RUN_ACCENT_HEX.toColorInt())
            .alpha(0.92f)
            .width(7f)
    )

    val isReplayMode = replayPointIndex != null
    map.addMarker(
        MarkerOptions()
            .position(points.first())
            .title(if (isReplayMode && points.size == 1) "Replay position" else "Start")
    )

    val currentPoint = points.last()
    val shouldAddSeparateMarker = currentPoint.latitude != points.first().latitude ||
        currentPoint.longitude != points.first().longitude
    if (shouldAddSeparateMarker) {
        map.addMarker(
            MarkerOptions()
                .position(currentPoint)
                .title(
                    when {
                        isReplayMode -> "Replay position"
                        points.size == decoded.size -> "Finish"
                        else -> "Current position"
                    }
                )
        )
    }

    if (followRoute) {
        val viewport = replayViewport ?: viewportCalculator.buildViewport(
            points.map { point -> RunViewportPoint(point.latitude, point.longitude) }
        ) ?: return
        val bounds = LatLngBounds.from(viewport.north, viewport.east, viewport.south, viewport.west)
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, viewport.paddingPx)
        if (replayPointIndex != null && replayPointIndex > 0) {
            map.animateCamera(cameraUpdate, 450)
        } else {
            map.moveCamera(cameraUpdate)
        }
    }
}
