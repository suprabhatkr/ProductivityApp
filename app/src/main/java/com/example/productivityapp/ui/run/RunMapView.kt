package com.example.productivityapp.ui.run

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import com.example.productivityapp.util.PolylineUtils

@Composable
fun RunMapView(polylineEncoded: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    AndroidView(factory = { context ->
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        val map = MapView(context)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        map.controller.setZoom(15.0)
        map
    }, modifier = modifier, update = { map ->
        // Remove existing polylines
        val overlays = map.overlays
        overlays.removeAll { it is Polyline }

        if (polylineEncoded.isNotBlank()) {
            val pts = PolylineUtils.decode(polylineEncoded).map { (lat, lon) -> GeoPoint(lat, lon) }
            if (pts.isNotEmpty()) {
                val poly = Polyline()
                poly.setPoints(pts)
                poly.outlinePaint.strokeWidth = 8f
                overlays.add(poly)
                map.invalidate()
                // center on last point
                map.controller.setCenter(pts.last())
            }
        }
    })
}

