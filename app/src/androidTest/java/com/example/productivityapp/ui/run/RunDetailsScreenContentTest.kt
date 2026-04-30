package com.example.productivityapp.ui.run

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RunDetailsScreenContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun nullRun_showsFallbackAndBackAction() {
        var backPressed = false

        composeRule.setContent {
            MaterialTheme {
                RunDetailsScreenContent(
                    run = null,
                    onBack = { backPressed = true },
                )
            }
        }

        composeRule.onNodeWithText("Run not found").assertIsDisplayed()
        composeRule.onNodeWithText("Back to Run").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertTrue(backPressed)
        }
    }

    @Test
    fun completedRun_withRouteData_showsReplayAndExportControls() {
        composeRule.setContent {
            MaterialTheme {
                RunDetailsScreenContent(
                    run = completedRun(),
                    runPoints = completedRunPoints(),
                )
            }
        }

        composeRule.onNodeWithText("Route").assertIsDisplayed()
        composeRule.onNodeWithText("Replay").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Run details replay position slider").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play run details replay").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reset run details replay").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Export run replay video").assertIsDisplayed()
    }

    @Test
    fun incompleteRun_withoutRouteData_explainsReplayUnavailable() {
        composeRule.setContent {
            MaterialTheme {
                RunDetailsScreenContent(
                    run = completedRun(polyline = "", endTime = null),
                    runPoints = emptyList(),
                )
            }
        }

        composeRule.onNodeWithText("This run does not have enough route data yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Summary").assertIsDisplayed()
    }

    private fun completedRun(
        polyline: String = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
        endTime: Long? = 8_000L,
    ): RunEntity = RunEntity(
        id = 7L,
        startTime = 1_700_000_000_000L,
        endTime = endTime,
        distanceMeters = 2450.0,
        durationSec = 605L,
        avgSpeedMps = 4.04,
        calories = 180.0,
        polyline = polyline,
    )

    private fun completedRunPoints(): List<RunPointEntity> = listOf(
        RunPointEntity(runId = 7L, lat = 38.5, lon = -120.2, tsMs = 1_000L),
        RunPointEntity(runId = 7L, lat = 40.7, lon = -120.95, tsMs = 3_000L),
        RunPointEntity(runId = 7L, lat = 43.252, lon = -126.453, tsMs = 6_000L),
    )
}
