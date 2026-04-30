package com.example.productivityapp.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMapViewportCalculatorTest {

    private val calculator = RunMapViewportCalculator()

    @Test
    fun buildViewport_appliesMinimumVisibleAreaToShortRoutes() {
        val viewport = requireNotNull(
            calculator.buildViewport(
                listOf(RunViewportPoint(12.9716, 77.5946))
            )
        )

        assertTrue(
            calculator.distanceMeters(
                viewport.south,
                viewport.centerLon,
                viewport.north,
                viewport.centerLon,
            ) >= 320.0
        )
        assertTrue(
            calculator.distanceMeters(
                viewport.centerLat,
                viewport.west,
                viewport.centerLat,
                viewport.east,
            ) >= 320.0
        )
        assertEquals(12.9716, viewport.centerLat, 1e-6)
        assertEquals(77.5946, viewport.centerLon, 1e-6)
    }

    @Test
    fun buildViewport_containsEntireRouteAndExpandsAsDistanceGrows() {
        val earlyViewport = requireNotNull(
            calculator.buildViewport(
                listOf(
                    RunViewportPoint(12.9716, 77.5946),
                    RunViewportPoint(12.9720, 77.5951),
                )
            )
        )
        val laterViewport = requireNotNull(
            calculator.buildViewport(
                listOf(
                    RunViewportPoint(12.9716, 77.5946),
                    RunViewportPoint(12.9720, 77.5951),
                    RunViewportPoint(12.9790, 77.6060),
                )
            )
        )

        assertContains(laterViewport, RunViewportPoint(12.9716, 77.5946))
        assertContains(laterViewport, RunViewportPoint(12.9720, 77.5951))
        assertContains(laterViewport, RunViewportPoint(12.9790, 77.6060))

        val earlyWidth = calculator.distanceMeters(
            earlyViewport.centerLat,
            earlyViewport.west,
            earlyViewport.centerLat,
            earlyViewport.east,
        )
        val laterWidth = calculator.distanceMeters(
            laterViewport.centerLat,
            laterViewport.west,
            laterViewport.centerLat,
            laterViewport.east,
        )

        assertTrue(laterWidth > earlyWidth)
        assertTrue(laterViewport.paddingPx >= earlyViewport.paddingPx)
    }

    @Test
    fun buildViewport_preservesMinimumWidthAtHighLatitudes() {
        val viewport = requireNotNull(
            calculator.buildViewport(
                listOf(RunViewportPoint(85.0, 20.0))
            )
        )

        val widthMeters = calculator.distanceMeters(
            viewport.centerLat,
            viewport.west,
            viewport.centerLat,
            viewport.east,
        )

        assertTrue(widthMeters >= 320.0)
        assertEquals(85.0, viewport.centerLat, 1e-6)
        assertEquals(20.0, viewport.centerLon, 1e-6)
    }

    private fun assertContains(viewport: RunMapViewport, point: RunViewportPoint) {
        assertTrue(point.latitude in viewport.south..viewport.north)
        assertTrue(point.longitude in viewport.west..viewport.east)
    }
}
