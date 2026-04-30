package com.example.productivityapp.ui.sleep

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.productivityapp.data.entities.SleepEntity
import org.junit.Rule
import org.junit.Test

class SleepScreenContentUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun heroRingAndHistory_shareScrollableContent() {
        composeRule.setContent {
            MaterialTheme {
                SleepScreenContent(
                    sessions = listOf(
                        SleepEntity(
                            id = 1L,
                            date = "2026-04-21",
                            startTimestamp = 1_000L,
                            endTimestamp = 10_000L,
                            durationSec = 9_000L,
                            sleepQuality = 4,
                            notes = "Felt good",
                            detectionSource = "manual",
                            confidenceScore = 1.0,
                            inferredStartTimestamp = null,
                            inferredEndTimestamp = null,
                            reviewState = "confirmed",
                            tagsCsv = "",
                        )
                    ),
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
        }

        composeRule.onNodeWithText("Tonight's progress").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_content_list").performScrollToNode(hasText("History"))
        composeRule.onNodeWithText("History").assertIsDisplayed()
    }
}
