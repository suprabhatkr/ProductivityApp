package com.example.productivityapp.ui.run

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class RunMapViewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val encodedFixture = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"

    @Test
    fun drawsPolylineAndMarkersForRecordedRun() {
        var mapView: MapView? = null

        composeRule.setContent {
            MaterialTheme {
                RunMapView(
                    polylineEncoded = encodedFixture,
                    onMapReady = { mapView = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            mapView?.overlays?.any { it is Polyline } == true
        }

        composeRule.runOnUiThread {
            val map = requireNotNull(mapView)
            val polyline = map.overlays.filterIsInstance<Polyline>().singleOrNull()
            val markers = map.overlays.filterIsInstance<Marker>()
            assertNotNull(polyline)
            assertTrue(markers.size >= 2)
        }
    }

    @Test
    fun replayModeCentersOnReplayPoint() {
        var mapView: MapView? = null

        composeRule.setContent {
            var replayIndex by remember { mutableIntStateOf(0) }
            MaterialTheme {
                RunMapView(
                    polylineEncoded = encodedFixture,
                    replayPointIndex = replayIndex,
                    followRoute = true,
                    onMapReady = { mapView = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            mapView?.overlays?.any { it is Polyline } == true
        }

        composeRule.runOnUiThread {
            val map = requireNotNull(mapView)
            val center = map.mapCenter as GeoPoint
            assertWithin(center.latitude, 38.5)
            assertWithin(center.longitude, -120.2)
            assertTrue(map.overlays.filterIsInstance<Marker>().any { it.title == "Replay position" })
        }
    }

    private fun assertWithin(actual: Double, expected: Double, epsilon: Double = 1e-4) {
        assertTrue("expected=$expected actual=$actual", abs(actual - expected) <= epsilon)
    }
}


