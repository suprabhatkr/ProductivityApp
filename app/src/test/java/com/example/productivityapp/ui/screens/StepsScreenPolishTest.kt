package com.example.productivityapp.ui.screens

import com.example.productivityapp.test.ComposeTestRuleHolder
import com.example.productivityapp.ui.steps.StepsHeader
import com.example.productivityapp.ui.steps.StepsProgress
import org.junit.Ignore
import org.junit.Test
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText

class StepsScreenPolishTest : ComposeTestRuleHolder() {

    @Ignore("Robolectric Compose idling NPE in JVM test environment")
    @Test
    fun headerAndProgress_render_and_have_semantics() {
        setThemedContent {
            TestStepsPreview()
        }

        // verify header and progress cards exist
        composeTestRule.onNodeWithTag("steps_header").assertExists()
        composeTestRule.onNodeWithTag("steps_progress").assertExists()
        // verify text content
        composeTestRule.onNodeWithText("Today: 4200").assertExists()
    }

    @Composable
    private fun TestStepsPreview() {
        Column {
            StepsHeader(steps = 4200, dailyGoal = 10000)
            StepsProgress(steps = 4200, dailyGoal = 10000)
        }
    }
}
