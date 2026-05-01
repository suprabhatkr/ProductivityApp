package com.example.productivityapp.app.viewmodel

import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.model.UserProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun buildHomeDashboardUiState_exposesLiveFeatureSummaries() {
        val date = LocalDate.of(2026, 4, 30)
        val uiState = buildHomeDashboardUiState(
            date = date,
            currentTime = LocalTime.of(18, 45),
            waterDay = WaterDayData(
                date = date.toString(),
                entries = listOf(
                    WaterEntry(id = 1L, amountMl = 500, timestamp = LocalDateTime.of(date, LocalTime.of(8, 0))),
                    WaterEntry(id = 2L, amountMl = 750, timestamp = LocalDateTime.of(date, LocalTime.of(13, 0))),
                ),
                goalMl = 2000,
            ),
            stepEntity = StepEntity(
                id = 1L,
                date = date.toString(),
                steps = 8200,
                distanceMeters = 5900.0,
                calories = 340.0,
                source = "sensor",
                lastUpdatedAt = 1L,
            ),
            runs = listOf(
                RunEntity(
                    id = 7L,
                    startTime = LocalDateTime.of(date, LocalTime.of(7, 30))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    endTime = LocalDateTime.of(date, LocalTime.of(8, 0))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    distanceMeters = 3200.0,
                    durationSec = 1800L,
                    avgSpeedMps = 1.77,
                    calories = 180.0,
                    polyline = "",
                )
            ),
            sleeps = listOf(
                SleepEntity(
                    id = 3L,
                    date = date.toString(),
                    startTimestamp = 1L,
                    endTimestamp = 2L,
                    durationSec = 7 * 3600L + 25 * 60L,
                    sleepQuality = 4,
                    notes = null,
                )
            ),
            profile = UserProfile(
                dailyStepGoal = 10000,
                dailyWaterGoalMl = 2000,
                nightlySleepGoalMinutes = 480,
            ),
        )

        assertEquals("Good evening", uiState.greetingTitle)
        assertEquals("1250 ml", uiState.waterSummary.headline)
        assertEquals("8,200 steps", uiState.stepsSummary.headline)
        assertEquals("3.20 km", uiState.runSummary.headline)
        assertEquals("7h 25m", uiState.sleepSummary.headline)
        assertTrue(uiState.heroChips.any { it.contains("Water 62%") })
    }
}
