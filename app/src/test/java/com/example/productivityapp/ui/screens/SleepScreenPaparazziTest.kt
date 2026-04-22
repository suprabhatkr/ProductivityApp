package com.example.productivityapp.ui.screens

import org.junit.Ignore
import org.junit.Test
import com.example.productivityapp.test.ComposeTestRuleHolder
import com.example.productivityapp.ui.sleep.SleepScreenContent
import com.example.productivityapp.viewmodel.SleepDaySummary
import com.example.productivityapp.data.entities.SleepEntity

class SleepScreenPaparazziTest : ComposeTestRuleHolder() {

    @Ignore("Robolectric Compose idling NPE in JVM test environment")
    @Test
    fun sleep_defaultState_snapshot() {
        val sessions = listOf(
            SleepEntity(id = 1L, date = "2026-04-10", startTimestamp = 0L, endTimestamp = 0L, durationSec = 0L, sleepQuality = null, notes = null)
        )

        val weekly = listOf(
            SleepDaySummary(date = "2026-04-04", label = "Mon", totalDurationSec = 7 * 3600, averageQuality = 4.0),
            SleepDaySummary(date = "2026-04-05", label = "Tue", totalDurationSec = 8 * 3600, averageQuality = 4.2),
            SleepDaySummary(date = "2026-04-06", label = "Wed", totalDurationSec = 6 * 3600, averageQuality = 3.8),
            SleepDaySummary(date = "2026-04-07", label = "Thu", totalDurationSec = 7 * 3600, averageQuality = 4.1),
            SleepDaySummary(date = "2026-04-08", label = "Fri", totalDurationSec = 5 * 3600, averageQuality = 3.5),
            SleepDaySummary(date = "2026-04-09", label = "Sat", totalDurationSec = 9 * 3600, averageQuality = 4.6),
            SleepDaySummary(date = "2026-04-10", label = "Sun", totalDurationSec = 8 * 3600, averageQuality = 4.3),
        )

        setThemedContent {
            SleepScreenContent(
                sessions = sessions,
                weeklySummary = weekly,
                activeSession = null,
                elapsedSeconds = 0L,
                isPaused = false,
                pendingReviewSession = null,
                pendingDetectedReviewSession = null,
                onLogSleep = { _, _, _, _ -> },
                onStartNapTimer = {},
                onScheduleWakeAlarm = { _, _ -> },
                onPauseSleep = {},
                onResumeSleep = {},
                onStopSleep = {},
                onSubmitReview = { _, _ -> },
                onDismissReview = {},
                onAcceptDetectedReview = {},
                onAdjustDetectedReview = { _, _, _ -> },
                onMergeDetectedReview = {},
                onDismissDetectedReview = {},
            )
        }
    }
}
