package com.example.productivityapp.ui.sleep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.productivityapp.test.ComposeTestRuleHolder
import com.example.productivityapp.viewmodel.SleepDaySummary
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
                onStartSleep = {},
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
        composeTestRule.onNodeWithText("Nap timer & wake alarm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Nap Timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set Wake Alarm").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
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
                onStartSleep = {},
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

        composeTestRule.onNodeWithContentDescription("Set wake-up alarm").performClick()
        composeTestRule.onNodeWithContentDescription("Use exact alarm switch").performClick()
        composeTestRule.onNodeWithText("Exact alarms are not currently available. You can open system settings to grant access, or keep the timed fallback.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open exact alarm settings").assertIsDisplayed()
    }
}
