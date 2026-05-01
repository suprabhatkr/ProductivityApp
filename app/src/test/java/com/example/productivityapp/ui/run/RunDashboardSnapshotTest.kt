package com.example.productivityapp.ui.run

import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.model.OutdoorActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RunDashboardSnapshotTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 4, 22)

    @Test
    fun inProgressSnapshot_aggregatesTodayAndWeeklyDistance() {
        val runs = listOf(
            runEntity(
                startTime = today.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3_600_000,
                endTime = null,
                distanceMeters = 2_000.0,
                durationSec = 900L,
            ),
            runEntity(
                startTime = today.atStartOfDay(zoneId).toInstant().toEpochMilli() + 7_200_000,
                endTime = today.atStartOfDay(zoneId).toInstant().toEpochMilli() + 9_000_000,
                distanceMeters = 1_500.0,
                durationSec = 780L,
            ),
            runEntity(
                startTime = today.minusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                endTime = today.minusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli() + 2_400_000,
                distanceMeters = 4_000.0,
                durationSec = 1_400L,
            ),
        )

        val snapshot = buildRunDashboardSnapshot(
            runs = runs,
            isTracking = true,
            zoneId = zoneId,
            today = today,
        )

        assertEquals("Run in progress", snapshot.statusLabel)
        assertEquals(3_500.0, snapshot.todayDistanceMeters, 0.0001)
        assertEquals(7_500.0, snapshot.weeklyDistanceMeters, 0.0001)
        assertEquals(3, snapshot.totalRuns)
        assertEquals(2, snapshot.completedRuns)
        assertEquals(0.7f, snapshot.todayGoalProgress, 0.0001f)
        assertTrue(snapshot.bestPaceLabel != null)
    }

    @Test
    fun pausedSnapshot_marksPausedWhenLatestRunIsIncomplete() {
        val runs = listOf(
            runEntity(
                startTime = today.atStartOfDay(zoneId).toInstant().toEpochMilli() + 1_000,
                endTime = null,
                distanceMeters = 800.0,
                durationSec = 420L,
                activityType = OutdoorActivityType.WALK,
            ),
            runEntity(
                startTime = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                endTime = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() + 1_800_000,
                distanceMeters = 5_200.0,
                durationSec = 1_500L,
            ),
        )

        val snapshot = buildRunDashboardSnapshot(
            runs = runs,
            isTracking = false,
            zoneId = zoneId,
            today = today,
        )

        assertEquals("Walk paused", snapshot.statusLabel)
        assertTrue(snapshot.hasPausedRun)
        assertEquals(5_200.0, snapshot.longestRunMeters, 0.0001)
        assertEquals("4:48 /km", snapshot.bestPaceLabel)
    }

    @Test
    fun emptySnapshot_showsReadyToStartAndZeroedMetrics() {
        val snapshot = buildRunDashboardSnapshot(
            runs = emptyList(),
            isTracking = false,
            zoneId = zoneId,
            today = today,
        )

        assertEquals("Ready to start", snapshot.statusLabel)
        assertTrue(snapshot.statusDetail.contains("Start a run or walk"))
        assertEquals(0.0, snapshot.todayDistanceMeters, 0.0001)
        assertEquals(0.0, snapshot.weeklyDistanceMeters, 0.0001)
        assertEquals(0, snapshot.totalRuns)
        assertEquals(0, snapshot.completedRuns)
        assertEquals(0f, snapshot.todayGoalProgress, 0.0001f)
        assertTrue(snapshot.bestPaceLabel == null)
        assertTrue(snapshot.hasPausedRun.not())
    }

    @Test
    fun hasPausedRun_onlyTrueForIncompleteLatestRunWhenNotTracking() {
        val incompleteRun = runEntity(
            startTime = today.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endTime = null,
            distanceMeters = 1200.0,
            durationSec = 400L,
        )
        val completedRun = incompleteRun.copy(endTime = incompleteRun.startTime + 1_000L)

        assertTrue(hasPausedRun(incompleteRun, isTracking = false))
        assertTrue(hasPausedRun(incompleteRun, isTracking = true).not())
        assertTrue(hasPausedRun(completedRun, isTracking = false).not())
        assertTrue(hasPausedRun(null, isTracking = false).not())
    }

    private fun runEntity(
        startTime: Long,
        endTime: Long?,
        distanceMeters: Double,
        durationSec: Long,
        activityType: OutdoorActivityType = OutdoorActivityType.RUN,
    ): RunEntity = RunEntity(
        activityType = activityType.storageValue,
        startTime = startTime,
        endTime = endTime,
        distanceMeters = distanceMeters,
        durationSec = durationSec,
        avgSpeedMps = if (durationSec > 0) distanceMeters / durationSec else 0.0,
        calories = 120.0,
        polyline = "",
    )
}
