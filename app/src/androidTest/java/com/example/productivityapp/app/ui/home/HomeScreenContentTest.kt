package com.example.productivityapp.app.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.productivityapp.app.viewmodel.HomeDashboardUiState
import com.example.productivityapp.app.viewmodel.HomeFeatureSummary
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeScreen_displaysLiveFeatureSummariesAndFooterLinks() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreenContent(
                    uiState = sampleHomeUiState(),
                    onNavigateToSteps = {},
                    onNavigateToStepsLegacy = {},
                    onNavigateToRun = {},
                    onNavigateToWorkout = {},
                    onNavigateToMindfulness = {},
                    onNavigateToSleep = {},
                    onNavigateToWater = {},
                    onNavigateToSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Health App").assertIsDisplayed()
        composeRule.onNodeWithText("Good evening").assertIsDisplayed()
        composeRule.onNodeWithText("750 ml").assertIsDisplayed()
        composeRule.onNodeWithText("8,200 steps").assertIsDisplayed()
        composeRule.onNodeWithText("3.20 km").assertIsDisplayed()
        composeRule.onNodeWithText("45m").assertIsDisplayed()
        composeRule.onNodeWithText("15m").assertIsDisplayed()
        composeRule.onNodeWithText("7h 25m").assertIsDisplayed()
        composeRule.onAllNodesWithText("Your daily snapshot across steps, runs, sleep, water, and preferences.").assertCountEquals(0)
        composeRule.onAllNodesWithText("37% of 2000 ml goal").assertCountEquals(0)
        composeRule.onAllNodesWithText("Open legacy Steps view").assertCountEquals(0)
        composeRule.onNodeWithText("Terms of Service").assertIsDisplayed()
        composeRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    @Test
    fun homeScreen_settingsAndPolicyLinksInvokeCallbacks() {
        var settingsOpened = false
        var termsOpened = false
        var privacyOpened = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreenContent(
                    uiState = sampleHomeUiState(),
                    onNavigateToSteps = {},
                    onNavigateToStepsLegacy = {},
                    onNavigateToRun = {},
                    onNavigateToWorkout = {},
                    onNavigateToMindfulness = {},
                    onNavigateToSleep = {},
                    onNavigateToWater = {},
                    onNavigateToSettings = { settingsOpened = true },
                    onOpenTermsOfService = { termsOpened = true },
                    onOpenPrivacyPolicy = { privacyOpened = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Terms of Service").performClick()
        composeRule.onNodeWithText("Privacy Policy").performClick()

        composeRule.runOnIdle {
            assertTrue(settingsOpened)
            assertTrue(termsOpened)
            assertTrue(privacyOpened)
        }
    }
}

private fun sampleHomeUiState(): HomeDashboardUiState {
    return HomeDashboardUiState(
        dateLabel = "Thursday, 30 April",
        greetingTitle = "Good evening",
        greetingMessage = "Your daily snapshot across steps, runs, sleep, water, and preferences.",
        heroChips = listOf("Water 37%", "Steps 82%", "Sleep 7h 25m"),
        waterSummary = HomeFeatureSummary(
            title = "Water intake",
            headline = "750 ml",
            supporting = "37% of 2000 ml goal",
            secondary = "1250 ml remaining",
            progressFraction = 0.37f,
        ),
        stepsSummary = HomeFeatureSummary(
            title = "Steps",
            headline = "8,200 steps",
            supporting = "82% of 10000 goal",
            secondary = "5.90 km",
            progressFraction = 0.82f,
        ),
        runSummary = HomeFeatureSummary(
            title = "Run",
            headline = "3.20 km",
            supporting = "1 run today",
            secondary = "Latest 5.00 km",
            progressFraction = 0.64f,
        ),
        workoutSummary = HomeFeatureSummary(
            title = "Workout",
            headline = "45m",
            supporting = "1 session today",
            secondary = "Latest Yoga",
            progressFraction = null,
        ),
        mindfulnessSummary = HomeFeatureSummary(
            title = "Mindfulness",
            headline = "15m",
            supporting = "1 session • 1 reflection",
            secondary = "Latest Breathing",
            progressFraction = null,
        ),
        sleepSummary = HomeFeatureSummary(
            title = "Sleep",
            headline = "7h 25m",
            supporting = "1 session today",
            secondary = "Goal 8h 00m",
            progressFraction = 0.93f,
        ),
        settingsSummary = HomeFeatureSummary(
            title = "Settings",
            headline = "10000 steps • 2000 ml",
            supporting = "8h 00m sleep goal",
            secondary = "Metric units",
            progressFraction = null,
        ),
    )
}
