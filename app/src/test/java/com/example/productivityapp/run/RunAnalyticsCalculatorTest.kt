package com.example.productivityapp.run

import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunAnalyticsCalculatorTest {

    private val calculator = RunAnalyticsCalculator()

    @Test
    fun buildSnapshot_createsSpeedSamplesAndSplits() {
        val run = RunEntity(
            startTime = 1_000L,
            endTime = 1_601_000L,
            distanceMeters = 1_500.0,
            durationSec = 600L,
            avgSpeedMps = 2.5,
            calories = 120.0,
            polyline = "",
        )
        val points = listOf(
            RunPointEntity(runId = 1L, lat = 12.9716, lon = 77.5946, tsMs = 1_000L),
            RunPointEntity(runId = 1L, lat = 12.9750, lon = 77.6000, tsMs = 301_000L),
            RunPointEntity(runId = 1L, lat = 12.9790, lon = 77.6060, tsMs = 601_000L),
        )

        val snapshot = calculator.buildSnapshot(run, points)

        assertEquals(600L, snapshot.movingDurationSec)
        assertEquals(1_600L, snapshot.elapsedDurationSec)
        assertFalse(snapshot.speedSamples.isEmpty())
        assertFalse(snapshot.splits.isEmpty())
        assertTrue(snapshot.maxSpeedMps >= snapshot.avgSpeedMps)
        assertTrue(snapshot.insights.isNotEmpty())
    }

    @Test
    fun buildSnapshot_handlesSparseDataGracefully() {
        val run = RunEntity(
            startTime = 10_000L,
            endTime = 20_000L,
            distanceMeters = 100.0,
            durationSec = 10L,
            avgSpeedMps = 10.0,
            calories = 12.0,
            polyline = "",
        )

        val snapshot = calculator.buildSnapshot(run, emptyList())

        assertTrue(snapshot.speedSamples.isEmpty())
        assertTrue(snapshot.splits.isEmpty())
        assertEquals(10L, snapshot.elapsedDurationSec)
        assertTrue(snapshot.insights.isNotEmpty())
    }
}
