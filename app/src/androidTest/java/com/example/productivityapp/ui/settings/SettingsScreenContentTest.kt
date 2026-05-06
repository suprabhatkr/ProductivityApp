package com.example.productivityapp.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarPickerFilter
import com.example.productivityapp.viewmodel.AvatarEditorState
import com.example.productivityapp.viewmodel.ProfileSettingsSectionState
import com.example.productivityapp.viewmodel.RunSettingsSectionState
import com.example.productivityapp.viewmodel.SettingsUiState
import com.example.productivityapp.viewmodel.SleepSettingsSectionState
import com.example.productivityapp.viewmodel.StepSettingsSectionState
import com.example.productivityapp.viewmodel.WaterSettingsSectionState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreen_displaysPolishedSections() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreenContent(
                    uiState = sampleSettingsUiState(),
                    onBack = {},
                    onDisplayNameChanged = {},
                    onAgeChanged = {},
                    onGenderChanged = {},
                    onHeightChanged = {},
                    onWeightChanged = {},
                    onDailyStepGoalChanged = {},
                    onStrideLengthChanged = {},
                    onPreferredUnitsChanged = {},
                    onDailyWaterGoalChanged = {},
                    onNightlySleepGoalChanged = {},
                    onTypicalBedtimeChanged = {},
                    onTypicalWakeTimeChanged = {},
                    onSleepDetectionBufferChanged = {},
                    onOpenAvatarEditor = {},
                    onDismissAvatarEditor = {},
                    onApplyAvatarDraft = {},
                    onAvatarFilterChanged = {},
                    onAvatarSelected = {},
                    onReset = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
        composeRule.onNodeWithText("Steps").assertIsDisplayed()
        composeRule.onNodeWithText("Run").assertIsDisplayed()
        composeRule.onNodeWithText("Water").assertIsDisplayed()
        composeRule.onNodeWithText("Sleep").assertIsDisplayed()
        composeRule.onNodeWithText("Privacy & storage").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backAndSaveCallbacksFire() {
        var backPressed = false
        var savePressed = false

        composeRule.setContent {
            MaterialTheme {
                SettingsScreenContent(
                    uiState = sampleSettingsUiState(),
                    onBack = { backPressed = true },
                    onDisplayNameChanged = {},
                    onAgeChanged = {},
                    onGenderChanged = {},
                    onHeightChanged = {},
                    onWeightChanged = {},
                    onDailyStepGoalChanged = {},
                    onStrideLengthChanged = {},
                    onPreferredUnitsChanged = {},
                    onDailyWaterGoalChanged = {},
                    onNightlySleepGoalChanged = {},
                    onTypicalBedtimeChanged = {},
                    onTypicalWakeTimeChanged = {},
                    onSleepDetectionBufferChanged = {},
                    onOpenAvatarEditor = {},
                    onDismissAvatarEditor = {},
                    onApplyAvatarDraft = {},
                    onAvatarFilterChanged = {},
                    onAvatarSelected = {},
                    onReset = {},
                    onSave = { savePressed = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertTrue(backPressed)
            assertTrue(savePressed)
        }
    }

    @Test
    fun settingsScreen_avatarPickerOpenApplyAndCancelFlowWorks() {
        composeRule.setContent {
            var state by mutableStateOf(sampleSettingsUiState())
            MaterialTheme {
                SettingsScreenContent(
                    uiState = state,
                    onBack = {},
                    onDisplayNameChanged = {},
                    onAgeChanged = {},
                    onGenderChanged = {},
                    onHeightChanged = {},
                    onWeightChanged = {},
                    onDailyStepGoalChanged = {},
                    onStrideLengthChanged = {},
                    onPreferredUnitsChanged = {},
                    onDailyWaterGoalChanged = {},
                    onNightlySleepGoalChanged = {},
                    onTypicalBedtimeChanged = {},
                    onTypicalWakeTimeChanged = {},
                    onSleepDetectionBufferChanged = {},
                    onOpenAvatarEditor = {
                        state = state.copy(
                            avatarEditor = AvatarEditorState(
                                isVisible = true,
                                draft = state.profile.avatar,
                                selectedFilter = AvatarPickerFilter.CREATURE,
                            )
                        )
                    },
                    onDismissAvatarEditor = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                isVisible = false,
                                draft = state.profile.avatar,
                                selectedFilter = AvatarPickerFilter.CREATURE,
                            )
                        )
                    },
                    onApplyAvatarDraft = {
                        state = state.copy(
                            profile = state.profile.copy(avatar = state.avatarEditor.draft),
                            avatarEditor = state.avatarEditor.copy(isVisible = false),
                        )
                    },
                    onAvatarFilterChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(selectedFilter = it),
                        )
                    },
                    onAvatarSelected = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(avatarId = it),
                            )
                        )
                    },
                    onReset = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Choose avatar").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Choose avatar").assertIsDisplayed()
        composeRule.onNodeWithText("Female").performClick()
        composeRule.onNodeWithText("Female 1").performClick()
        composeRule.onNodeWithText("Apply").performClick()
        composeRule.onNodeWithContentDescription(
            "Avatar preview, Female 1, female style",
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Choose avatar").performClick()
        composeRule.onNodeWithText("Male").performClick()
        composeRule.onNodeWithText("Male 2").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription(
            "Avatar preview, Female 1, female style",
        ).assertIsDisplayed()
    }
}

private fun sampleSettingsUiState(): SettingsUiState {
    return SettingsUiState(
        isLoading = false,
        hasUnsavedChanges = true,
        profile = ProfileSettingsSectionState(
            displayName = "Alex",
            ageYears = "30",
            gender = "Female",
            heightCm = "170",
            weightKg = "64.5",
            avatar = AvatarConfig(avatarId = "creature_01"),
        ),
        steps = StepSettingsSectionState(
            dailyStepGoal = "9000",
            strideLengthMeters = "0.79",
        ),
        run = RunSettingsSectionState(
            preferredUnits = "metric",
        ),
        water = WaterSettingsSectionState(
            dailyWaterGoalMl = "2400",
        ),
        sleep = SleepSettingsSectionState(
            nightlySleepGoalMinutes = "450",
            typicalBedtime = "22:30",
            typicalWakeTime = "06:45",
            sleepDetectionBufferMinutes = "30",
        ),
    )
}
