package com.example.productivityapp.ui.steps

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class StepScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manualEntryIsAvailableWhenSensorAbsent() {
        composeRule.setContent {
            StepScreenContent(
                steps = 123,
                dailyGoal = 10000,
                serviceRunning = false,
                permissionUiState = StepPermissionUiState.RequestOrSettings,
                hasStepSensor = false,
                onAddManualSteps = {},
                onStartService = {},
                onStopService = {},
                onRequestPermission = {},
                onOpenSettings = {},
            )
        }

        composeRule.onNodeWithText("Automatic step tracking unavailable").assertIsDisplayed()
        composeRule.onNodeWithTag("custom_step_input").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_100").assertIsDisplayed()
    }

    @Test
    fun permissionFallbackButtonsAppear() {
        composeRule.setContent {
            StepScreenContent(
                steps = 0,
                dailyGoal = 10000,
                serviceRunning = false,
                permissionUiState = StepPermissionUiState.RequestOrSettings,
                hasStepSensor = true,
                onAddManualSteps = {},
                onStartService = {},
                onStopService = {},
                onRequestPermission = {},
                onOpenSettings = {},
            )
        }

        composeRule.onNodeWithTag("request_permission_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_settings_button").assertIsDisplayed()
    }
}


