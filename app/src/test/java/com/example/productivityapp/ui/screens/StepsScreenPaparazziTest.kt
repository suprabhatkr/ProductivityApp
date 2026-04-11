package com.example.productivityapp.ui.screens

import org.junit.Test
import com.example.productivityapp.test.ComposeTestRuleHolder
import com.example.productivityapp.ui.steps.StepScreenContent

class StepsScreenPaparazziTest : ComposeTestRuleHolder() {

    @Test
    fun steps_defaultState_snapshot() {
        // Compose the StepScreenContent with representative fake state
        setThemedContent {
            StepScreenContent(
                steps = 1234,
                dailyGoal = 10000,
                serviceRunning = false,
                permissionUiState = com.example.productivityapp.ui.steps.StepPermissionUiState.Granted,
                hasStepSensor = true,
                onAddManualSteps = {},
                onStartService = {},
                onStopService = {},
                onRequestPermission = {},
                onOpenSettings = {}
            )
        }
    }
}


