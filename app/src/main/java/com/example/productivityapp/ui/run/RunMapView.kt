package com.example.productivityapp.ui.run

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.productivityapp.util.PolylineUtils
import com.example.productivityapp.ui.theme.RUN_ACCENT_HEX

internal const val RUN_MAP_CONTENT_DESCRIPTION = "Run route map"

@Composable
fun RunMapView(
    polylineEncoded: String,
    modifier: Modifier = Modifier,
    replayPointIndex: Int? = null,
    followRoute: Boolean = true,
    onMapReady: ((MapView) -> Unit)? = null,
) {
    val decodedPoints = remember(polylineEncoded) {
        if (polylineEncoded.isBlank()) emptyList() else PolylineUtils.decode(polylineEncoded)
    }

    AndroidView(factory = { context ->
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        val map = MapView(context)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        map.controller.setZoom(15.0)
        map
    }, modifier = modifier.semantics {
        contentDescription = "$RUN_MAP_CONTENT_DESCRIPTION (${decodedPoints.size} points)"
    }, update = { map ->
        onMapReady?.invoke(map)
        applyRunRouteOverlays(
            map = map,
            decoded = decodedPoints,
            replayPointIndex = replayPointIndex,
            followRoute = followRoute,
        )
    })
}

internal fun applyRunRouteOverlays(
    map: MapView,
    decoded: List<Pair<Double, Double>>,
    replayPointIndex: Int?,
    followRoute: Boolean,
) {
    val overlays = map.overlays
    overlays.removeAll { it is Polyline || it is Marker }

    if (decoded.isEmpty()) {
        map.invalidate()
        return
    }

    val visiblePairs = if (replayPointIndex != null && replayPointIndex >= 0) {
        decoded.take((replayPointIndex + 1).coerceAtMost(decoded.size))
    } else {
        decoded
    }
    val pts = visiblePairs.map { (lat, lon) -> GeoPoint(lat, lon) }
    if (pts.isEmpty()) {
        map.invalidate()
        return
    }

    val polyline = Polyline(map).apply {
        setPoints(pts)
        outlinePaint.strokeWidth = 10f
        outlinePaint.color = RUN_ACCENT_HEX.toColorInt()
        outlinePaint.alpha = 230
        outlinePaint.isAntiAlias = true
    }
    overlays.add(polyline)

    val startMarker = Marker(map).apply {
        position = pts.first()
        title = "Start"
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    overlays.add(startMarker)

    val currentPoint = pts.last()
    val shouldAddSeparateCurrentMarker = currentPoint.latitude != pts.first().latitude ||
        currentPoint.longitude != pts.first().longitude
    if (shouldAddSeparateCurrentMarker) {
        overlays.add(
            Marker(map).apply {
                position = currentPoint
                title = if (replayPointIndex != null) "Replay position" else "Current position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )
    } else {
        startMarker.title = if (replayPointIndex != null) "Replay position" else "Current position"
    }

    if (followRoute) {
        if (pts.size == 1 || replayPointIndex != null) {
            map.controller.setCenter(currentPoint)
            map.controller.setZoom(17.0)
        } else {
            map.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(pts), false, 96)
        }
    }
    map.invalidate()
}

