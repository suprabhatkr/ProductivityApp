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
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class RunMapViewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val encodedFixture = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"

    @Test
    fun drawsPolylineAndMarkersForRecordedRun() {
        var map: MapLibreMap? = null

        composeRule.setContent {
            MaterialTheme {
                RunMapView(
                    polylineEncoded = encodedFixture,
                    onMapReady = { map = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            map?.annotations?.any { it is Polyline } == true
        }

        composeRule.runOnUiThread {
            val readyMap = requireNotNull(map)
            val polyline = readyMap.annotations.filterIsInstance<Polyline>().firstOrNull()
            val markers = readyMap.annotations.filterIsInstance<Marker>()
            assertNotNull(polyline)
            assertTrue(markers.size >= 2)
        }
    }

    @Test
    fun replayModeCentersOnReplayPoint() {
        var map: MapLibreMap? = null

        composeRule.setContent {
            var replayIndex by remember { mutableIntStateOf(0) }
            MaterialTheme {
                RunMapView(
                    polylineEncoded = encodedFixture,
                    replayPointIndex = replayIndex,
                    followRoute = true,
                    onMapReady = { map = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            map?.annotations?.any { it is Polyline } == true
        }

        composeRule.runOnUiThread {
            val readyMap = requireNotNull(map)
            val center = requireNotNull(readyMap.cameraPosition.target)
            assertWithin(center.latitude, 38.5)
            assertWithin(center.longitude, -120.2)
            assertTrue(readyMap.annotations.filterIsInstance<Marker>().any { it.title == "Replay position" })
        }
    }

    private fun assertWithin(actual: Double, expected: Double, epsilon: Double = 1e-4) {
        assertTrue("expected=$expected actual=$actual", abs(actual - expected) <= epsilon)
    }
}
