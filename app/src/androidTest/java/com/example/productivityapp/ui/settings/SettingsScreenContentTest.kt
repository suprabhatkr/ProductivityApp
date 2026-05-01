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
import com.example.productivityapp.data.model.AvatarGlassesStyle
import com.example.productivityapp.data.model.AvatarHatStyle
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
                    onResetAvatarDraftToSaved = {},
                    onAvatarSkinToneChanged = {},
                    onAvatarPresentationChanged = {},
                    onAvatarHairStyleChanged = {},
                    onAvatarGlassesStyleChanged = {},
                    onAvatarHatStyleChanged = {},
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
                    onResetAvatarDraftToSaved = {},
                    onAvatarSkinToneChanged = {},
                    onAvatarPresentationChanged = {},
                    onAvatarHairStyleChanged = {},
                    onAvatarGlassesStyleChanged = {},
                    onAvatarHatStyleChanged = {},
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
    fun settingsScreen_avatarEditorOpenApplyAndCancelFlowWorks() {
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
                            )
                        )
                    },
                    onDismissAvatarEditor = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                isVisible = false,
                                draft = state.profile.avatar,
                            )
                        )
                    },
                    onApplyAvatarDraft = {
                        state = state.copy(
                            profile = state.profile.copy(avatar = state.avatarEditor.draft),
                            avatarEditor = state.avatarEditor.copy(isVisible = false),
                        )
                    },
                    onResetAvatarDraftToSaved = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(draft = state.profile.avatar),
                        )
                    },
                    onAvatarSkinToneChanged = {},
                    onAvatarPresentationChanged = {},
                    onAvatarHairStyleChanged = {},
                    onAvatarGlassesStyleChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(glassesStyle = it),
                            )
                        )
                    },
                    onAvatarHatStyleChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(hatStyle = it),
                            )
                        )
                    },
                    onReset = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Edit avatar").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Edit avatar").assertIsDisplayed()
        composeRule.onNodeWithText("Bold Frame").performClick()
        composeRule.onNodeWithText("Beanie").performClick()
        composeRule.onNodeWithText("Apply").performClick()
        composeRule.onNodeWithContentDescription(
            "Avatar preview, Medium skin, neutral look, short hair, bold frame glasses, beanie hat",
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Edit avatar").performClick()
        composeRule.onNodeWithText("Sun Hat").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription(
            "Avatar preview, Medium skin, neutral look, short hair, bold frame glasses, beanie hat",
        ).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_avatarEditorTabSwitchAndResetFlowWorks() {
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
                            )
                        )
                    },
                    onDismissAvatarEditor = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                isVisible = false,
                                draft = state.profile.avatar,
                            )
                        )
                    },
                    onApplyAvatarDraft = {
                        state = state.copy(
                            profile = state.profile.copy(avatar = state.avatarEditor.draft),
                            avatarEditor = state.avatarEditor.copy(isVisible = false),
                        )
                    },
                    onResetAvatarDraftToSaved = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(draft = state.profile.avatar),
                        )
                    },
                    onAvatarSkinToneChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(skinTone = it),
                            )
                        )
                    },
                    onAvatarPresentationChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(presentation = it),
                            )
                        )
                    },
                    onAvatarHairStyleChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(hairStyle = it),
                            )
                        )
                    },
                    onAvatarGlassesStyleChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(glassesStyle = it),
                            )
                        )
                    },
                    onAvatarHatStyleChanged = {
                        state = state.copy(
                            avatarEditor = state.avatarEditor.copy(
                                draft = state.avatarEditor.draft.copy(hatStyle = it),
                            )
                        )
                    },
                    onReset = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Edit avatar").performClick()
        composeRule.onNodeWithText("Hair").performClick()
        composeRule.onNodeWithText("Bun").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Look").performClick()
        composeRule.onNodeWithText("Masculine").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Skin").performClick()
        composeRule.onNodeWithText("Dark").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Hats").performClick()
        composeRule.onNodeWithText("Sun Hat").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Reset avatar draft").performClick()
        composeRule.onNodeWithContentDescription("Apply avatar changes").performClick()

        composeRule.onNodeWithContentDescription(
            "Avatar preview, Medium skin, neutral look, short hair, none glasses, none hat",
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
