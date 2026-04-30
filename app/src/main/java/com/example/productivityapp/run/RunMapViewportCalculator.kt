package com.example.productivityapp.run

import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class RunViewportPoint(
    val latitude: Double,
    val longitude: Double,
)

data class RunMapViewport(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val paddingPx: Int,
) {
    val centerLat: Double = (south + north) / 2.0
    val centerLon: Double = (west + east) / 2.0
}

class RunMapViewportCalculator(
    private val minimumVisibleSpanMeters: Double = 320.0,
    private val contentPaddingFraction: Double = 0.12,
    private val minPaddingPx: Int = 72,
    private val maxPaddingPx: Int = 120,
) {
    fun buildViewport(points: List<RunViewportPoint>): RunMapViewport? {
        if (points.isEmpty()) return null

        var south = points.first().latitude
        var north = points.first().latitude
        var west = points.first().longitude
        var east = points.first().longitude

        points.drop(1).forEach { point ->
            south = min(south, point.latitude)
            north = max(north, point.latitude)
            west = min(west, point.longitude)
            east = max(east, point.longitude)
        }

        val centerLat = (south + north) / 2.0
        val centerLon = (west + east) / 2.0
        return buildViewportFromBounds(
            south = south,
            west = west,
            north = north,
            east = east,
            centerLat = centerLat,
            centerLon = centerLon,
        )
    }

    internal fun buildViewport(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
    ): RunMapViewport {
        val centerLat = (south + north) / 2.0
        val centerLon = (west + east) / 2.0
        return buildViewportFromBounds(
            south = south,
            west = west,
            north = north,
            east = east,
            centerLat = centerLat,
            centerLon = centerLon,
        )
    }

    private fun buildViewportFromBounds(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        centerLat: Double,
        centerLon: Double,
    ): RunMapViewport {
        val latSpanMeters = distanceMeters(south, centerLon, north, centerLon)
        val lonSpanMeters = distanceMeters(centerLat, west, centerLat, east)
        val targetSpanMeters = max(max(latSpanMeters, lonSpanMeters), minimumVisibleSpanMeters)
        val paddedSpanMeters = targetSpanMeters * (1.0 + contentPaddingFraction * 2.0)
        val halfLatSpan = metersToLatitudeDegrees(paddedSpanMeters / 2.0)
        val halfLonSpan = metersToLongitudeDegrees(paddedSpanMeters / 2.0, centerLat)
            .coerceAtMost(MAX_HALF_LONGITUDE_DEGREES)
        val dynamicPaddingPx = (
            minPaddingPx +
                ((paddedSpanMeters - minimumVisibleSpanMeters) / 2_000.0)
                    .coerceIn(0.0, 1.0) * (maxPaddingPx - minPaddingPx)
            ).roundToInt()

        return RunMapViewport(
            south = centerLat - halfLatSpan,
            west = centerLon - halfLonSpan,
            north = centerLat + halfLatSpan,
            east = centerLon + halfLonSpan,
            paddingPx = dynamicPaddingPx,
        )
    }

    internal fun distanceMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
    ): Double {
        val lat1 = startLat.toRadians()
        val lat2 = endLat.toRadians()
        val deltaLat = (endLat - startLat).toRadians()
        val deltaLon = (endLon - startLon).toRadians()

        val a = kotlin.math.sin(deltaLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * kotlin.math.sin(deltaLon / 2).let { it * it }
        val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun metersToLatitudeDegrees(meters: Double): Double {
        return meters / METERS_PER_DEGREE_LATITUDE
    }

    private fun metersToLongitudeDegrees(meters: Double, latitude: Double): Double {
        val scale = max(MIN_LONGITUDE_SCALE, cos(latitude.toRadians()).absoluteValue)
        return meters / (METERS_PER_DEGREE_LATITUDE * scale)
    }

    private fun Double.toRadians(): Double = this / 180.0 * PI

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val METERS_PER_DEGREE_LATITUDE = 111_320.0
        const val MIN_LONGITUDE_SCALE = 1e-4
        const val MAX_HALF_LONGITUDE_DEGREES = 180.0
    }
}
