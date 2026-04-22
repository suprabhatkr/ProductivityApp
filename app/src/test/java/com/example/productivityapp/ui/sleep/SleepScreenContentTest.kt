package com.example.productivityapp.ui.sleep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.test.ComposeTestRuleHolder
import com.example.productivityapp.viewmodel.SleepDaySummary
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@Ignore("Robolectric Compose idling NPE in JVM test environment")
class SleepScreenContentTest : ComposeTestRuleHolder() {

    @Test
    fun defaultState_showsProgressAndActionSection() {
        setThemedContent {
            SleepScreenContent(
                sessions = emptyList(),
                weeklySummary = emptyList(),
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

        composeTestRule.onNodeWithText("Tonight's progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to log sleep").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Nap").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alarm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log Sleep").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sleep tip").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun logSleep_opensManualEntryDialog() {
        setThemedContent {
            SleepScreenContent(
                sessions = emptyList(),
                weeklySummary = emptyList(),
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

        composeTestRule.onNodeWithContentDescription("Log sleep").performClick()
        composeTestRule.onNodeWithText("Log sleep manually").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start date").assertIsDisplayed()
        composeTestRule.onNodeWithText("End date").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start time").assertIsDisplayed()
        composeTestRule.onNodeWithText("End time").assertIsDisplayed()
    }

    @Test
    fun activeNap_changesPrimaryButtonToStopNap() {
        var stopCalled = false

        setThemedContent {
            SleepScreenContent(
                sessions = emptyList(),
                weeklySummary = emptyList(),
                activeSession = activeNapSession(),
                elapsedSeconds = 5_000L,
                isPaused = false,
                pendingReviewSession = null,
                pendingDetectedReviewSession = null,
                onLogSleep = { _, _, _, _ -> },
                onStartNapTimer = {},
                onScheduleWakeAlarm = { _, _ -> },
                onPauseSleep = {},
                onResumeSleep = {},
                onStopSleep = { stopCalled = true },
                onSubmitReview = { _, _ -> },
                onDismissReview = {},
                onAcceptDetectedReview = {},
                onAdjustDetectedReview = { _, _, _ -> },
                onMergeDetectedReview = {},
                onDismissDetectedReview = {},
            )
        }

        composeTestRule.onNodeWithText("Stop Nap").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop nap").performClick()
        composeTestRule.runOnIdle {
            assertTrue(stopCalled)
        }
    }

    @Test
    fun sleepTip_canBeDismissedFromTheCard() {
        setThemedContent {
            SleepScreenContent(
                sessions = emptyList(),
                weeklySummary = emptyList(),
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

        composeTestRule.onNodeWithContentDescription("Dismiss sleep tip").performClick()
        composeTestRule.onAllNodesWithText("Sleep tip").assertCountEquals(0)
    }

    @Test
    fun wakeAlarmDialog_whenExactUnavailable_showsFallbackMessage() {
        setThemedContent {
            SleepScreenContent(
                sessions = emptyList(),
                weeklySummary = listOf(
                    SleepDaySummary(
                        date = "2026-04-20",
                        label = "Mon",
                        totalDurationSec = 7 * 3600,
                        averageQuality = 4.2,
                    ),
                ),
                activeSession = null,
                elapsedSeconds = 0L,
                isPaused = false,
                pendingReviewSession = null,
                pendingDetectedReviewSession = null,
                canUseExactAlarm = false,
                onRequestExactAlarmAccess = {},
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

        composeTestRule.onNodeWithContentDescription("Alarm").performClick()
        composeTestRule.onNodeWithContentDescription("Use exact alarm switch").performClick()
        composeTestRule.onNodeWithText("Exact alarms are not currently available. You can open system settings to grant access, or keep the timed fallback.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open exact alarm settings").assertIsDisplayed()
    }
}

private fun activeNapSession(): SleepEntity = SleepEntity(
    id = 42L,
    date = "2026-04-21",
    startTimestamp = 1_000L,
    endTimestamp = 0L,
    durationSec = 0L,
    sleepQuality = null,
    notes = null,
    detectionSource = SleepDetectionSource.NAP.storageValue,
    confidenceScore = 1.0,
    inferredStartTimestamp = 1_000L,
    inferredEndTimestamp = null,
    reviewState = SleepReviewState.CONFIRMED.storageValue,
    tagsCsv = "nap",
)
