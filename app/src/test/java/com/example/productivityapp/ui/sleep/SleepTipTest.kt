package com.example.productivityapp.ui.sleep

import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTipTest {
    @Test
    fun buildSleepTip_prioritizesActiveSessionAdvice() {
        val tip = buildSleepTip(
            activeSession = activeSleep(),
            pendingDetectedReviewSession = null,
            averageQuality = 2.0,
            bedtimeDeviationMinutes = 60.0,
            wakeDeviationMinutes = 10.0,
            napCount = 1,
        )

        assertEquals("Let the session run", tip.title)
    }

    @Test
    fun buildSleepTip_flagsLowQualityBeforeGenericAdvice() {
        val tip = buildSleepTip(
            activeSession = null,
            pendingDetectedReviewSession = null,
            averageQuality = 3.0,
            bedtimeDeviationMinutes = 10.0,
            wakeDeviationMinutes = 10.0,
            napCount = 0,
        )

        assertEquals("Sleep quality is running low", tip.title)
    }

    @Test
    fun buildSleepTip_returnsGenericAdviceWhenNothingElseStandsOut() {
        val tip = buildSleepTip(
            activeSession = null,
            pendingDetectedReviewSession = null,
            averageQuality = 4.3,
            bedtimeDeviationMinutes = 12.0,
            wakeDeviationMinutes = 10.0,
            napCount = 0,
        )

        assertEquals("Keep the routine steady", tip.title)
    }
}

private fun activeSleep(): SleepEntity = SleepEntity(
    id = 1L,
    date = "2026-04-17",
    startTimestamp = 1_000L,
    endTimestamp = 0L,
    durationSec = 0L,
    sleepQuality = null,
    notes = null,
    detectionSource = SleepDetectionSource.MANUAL.storageValue,
    confidenceScore = 1.0,
    inferredStartTimestamp = null,
    inferredEndTimestamp = null,
    reviewState = SleepReviewState.CONFIRMED.storageValue,
    tagsCsv = null,
)
