package com.example.productivityapp.ui.run

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.productivityapp.data.entities.RunEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RunScreenContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyDashboard_showsLocationEducationAndHistoryEmptyState() {
        composeRule.setContent {
            MaterialTheme {
                RunScreenContent(
                    runs = emptyList(),
                    isTracking = false,
                    permissionUiState = RunPermissionUiState(
                        primaryActionLabel = "Enable Location",
                        primaryAction = RunPrimaryAction.REQUEST_LOCATION,
                        permissionCard = RunPermissionCardModel(
                            title = "Enable precise location",
                            message = "Ask for location only when you want to start a run. It powers route mapping, pace, and distance.",
                            primaryLabel = "Grant location",
                            secondaryLabel = "App settings",
                            action = RunPermissionCardAction.REQUEST_LOCATION,
                        ),
                        shouldRequestNotificationsBeforeTracking = false,
                    ),
                    onOpenAppSettings = {},
                    onPrimaryRunAction = {},
                    onPauseRun = {},
                    onResumeRun = {},
                    onPermissionCardAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Run controls").assertIsDisplayed()
        composeRule.onNodeWithText("Enable precise location").assertIsDisplayed()
    }

    @Test
    fun latestRoute_showsDedicatedDetailsEntryPoint() {
        val latestRun = runEntity(
            id = 42L,
            distanceMeters = 3500.0,
            durationSec = 1260L,
            polyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
            endTime = 5_000L,
        )

        composeRule.setContent {
            MaterialTheme {
                RunScreenContent(
                    runs = listOf(latestRun),
                    isTracking = false,
                    permissionUiState = RunPermissionUiState(
                        primaryActionLabel = "Start Run",
                        primaryAction = RunPrimaryAction.START_OR_RESUME_RUN,
                        permissionCard = null,
                        shouldRequestNotificationsBeforeTracking = false,
                    ),
                    onOpenAppSettings = {},
                    onPrimaryRunAction = {},
                    onPauseRun = {},
                    onResumeRun = {},
                    onPermissionCardAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Latest route").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open latest run details").assertIsDisplayed()
    }

    @Test
    fun latestRoute_withoutPolylineStillAllowsOpeningDetails() {
        var openedRunId: Long? = null
        val latestRun = runEntity(
            id = 42L,
            distanceMeters = 3500.0,
            durationSec = 1260L,
            polyline = "",
            endTime = null,
        )

        composeRule.setContent {
            MaterialTheme {
                RunScreenContent(
                    runs = listOf(latestRun),
                    isTracking = false,
                    permissionUiState = RunPermissionUiState(
                        primaryActionLabel = "Start Run",
                        primaryAction = RunPrimaryAction.START_OR_RESUME_RUN,
                        permissionCard = null,
                        shouldRequestNotificationsBeforeTracking = false,
                    ),
                    onOpenAppSettings = {},
                    onPrimaryRunAction = {},
                    onPauseRun = {},
                    onResumeRun = {},
                    onPermissionCardAction = {},
                    onOpenRunDetails = { openedRunId = it },
                )
            }
        }

        composeRule.onNodeWithText("Live route").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open latest run details").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(42L, openedRunId)
        }
    }

    @Test
    fun pausedRun_showsResumeAction() {
        val pausedRun = runEntity(
            id = 9L,
            distanceMeters = 1200.0,
            durationSec = 420L,
            polyline = "",
            endTime = null,
        )
        var resumed = false

        composeRule.setContent {
            MaterialTheme {
                RunScreenContent(
                    runs = listOf(pausedRun),
                    isTracking = false,
                    permissionUiState = RunPermissionUiState(
                        primaryActionLabel = "Start Run",
                        primaryAction = RunPrimaryAction.START_OR_RESUME_RUN,
                        permissionCard = null,
                        shouldRequestNotificationsBeforeTracking = false,
                    ),
                    onOpenAppSettings = {},
                    onPrimaryRunAction = {},
                    onPauseRun = {},
                    onResumeRun = { resumed = true },
                    onPermissionCardAction = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Run paused")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Resume run").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertTrue(resumed)
        }
    }

    private fun runEntity(
        id: Long,
        distanceMeters: Double,
        durationSec: Long,
        polyline: String,
        endTime: Long?,
    ): RunEntity = RunEntity(
        id = id,
        startTime = 1_700_000_000_000L + id,
        endTime = endTime,
        distanceMeters = distanceMeters,
        durationSec = durationSec,
        avgSpeedMps = if (durationSec > 0L) distanceMeters / durationSec else 0.0,
        calories = 150.0,
        polyline = polyline,
    )
}
