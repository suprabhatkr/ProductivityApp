package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.model.AppThemePreference
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarGlassesStyle
import com.example.productivityapp.data.model.AvatarHairStyle
import com.example.productivityapp.data.model.AvatarHatStyle
import com.example.productivityapp.data.model.AvatarPresentation
import com.example.productivityapp.data.model.AvatarSkinTone
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.AppThemeRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsProfileAndThemeIntoSectionState() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Suprabhat",
                weightKg = 72.5,
                heightCm = 178,
                strideLengthMeters = 0.82,
                preferredUnits = "imperial",
                dailyStepGoal = 12000,
                dailyWaterGoalMl = 2600,
                nightlySleepGoalMinutes = 450,
                typicalBedtimeMinutes = 1380,
                typicalWakeTimeMinutes = 420,
                sleepDetectionBufferMinutes = 45,
                ageYears = 31,
                gender = "Male",
                avatar = AvatarConfig(
                    skinTone = AvatarSkinTone.MEDIUM_DARK,
                    presentation = AvatarPresentation.MASCULINE,
                    hairStyle = AvatarHairStyle.SPIKY,
                    glassesStyle = AvatarGlassesStyle.BOLD,
                    hatStyle = AvatarHatStyle.CAP,
                ),
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.DARK)

        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Suprabhat", state.profile.displayName)
        assertEquals("31", state.profile.ageYears)
        assertEquals("Male", state.profile.gender)
        assertEquals("72.5", state.profile.weightKg)
        assertEquals("178", state.profile.heightCm)
        assertEquals("0.82", state.steps.strideLengthMeters)
        assertEquals("imperial", state.run.preferredUnits)
        assertEquals("12000", state.steps.dailyStepGoal)
        assertEquals("2600", state.water.dailyWaterGoalMl)
        assertEquals("450", state.sleep.nightlySleepGoalMinutes)
        assertEquals("23:00", state.sleep.typicalBedtime)
        assertEquals("07:00", state.sleep.typicalWakeTime)
        assertEquals("45", state.sleep.sleepDetectionBufferMinutes)
        assertEquals(AvatarHatStyle.CAP, state.profile.avatar.hatStyle)
        assertEquals(AvatarHairStyle.SPIKY, state.avatarEditor.draft.hairStyle)
        assertEquals(AppThemePreference.DARK, state.appearance.themePreference)
    }

    @Test
    fun saveSettings_updatesRepositoriesAndClearsDirtyFlag() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(UserProfile())
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.updateDisplayName("Runner")
        viewModel.updateAgeYears("29")
        viewModel.updateGender("Female")
        viewModel.updateWeightKg("70.0")
        viewModel.updateHeightCm("175")
        viewModel.updateStrideLengthMeters("0.81")
        viewModel.updatePreferredUnits("imperial")
        viewModel.updateThemePreference(AppThemePreference.LIGHT)
        viewModel.updateDailyStepGoal("9000")
        viewModel.updateDailyWaterGoalMl("2300")
        viewModel.updateNightlySleepGoalMinutes("420")
        viewModel.updateTypicalBedtime("22:15")
        viewModel.updateTypicalWakeTime("06:45")
        viewModel.updateSleepDetectionBufferMinutes("25")
        viewModel.openAvatarEditor()
        viewModel.updateAvatarSkinTone(AvatarSkinTone.DARK)
        viewModel.updateAvatarPresentation(AvatarPresentation.FEMININE)
        viewModel.updateAvatarHairStyle(AvatarHairStyle.CURLY)
        viewModel.updateAvatarGlassesStyle(AvatarGlassesStyle.ROUND)
        viewModel.updateAvatarHatStyle(AvatarHatStyle.BEANIE)
        viewModel.applyAvatarDraft()
        viewModel.saveSettings()
        runCurrent()

        assertEquals("Runner", profileRepository.profile.value.displayName)
        assertEquals(29, profileRepository.profile.value.ageYears)
        assertEquals("Female", profileRepository.profile.value.gender)
        assertEquals(70.0, profileRepository.profile.value.weightKg)
        assertEquals(175, profileRepository.profile.value.heightCm)
        assertEquals(0.81, profileRepository.profile.value.strideLengthMeters, 0.0)
        assertEquals("imperial", profileRepository.profile.value.preferredUnits)
        assertEquals(9000, profileRepository.profile.value.dailyStepGoal)
        assertEquals(2300, profileRepository.profile.value.dailyWaterGoalMl)
        assertEquals(420, profileRepository.profile.value.nightlySleepGoalMinutes)
        assertEquals(22 * 60 + 15, profileRepository.profile.value.typicalBedtimeMinutes)
        assertEquals(6 * 60 + 45, profileRepository.profile.value.typicalWakeTimeMinutes)
        assertEquals(25, profileRepository.profile.value.sleepDetectionBufferMinutes)
        assertEquals(
            AvatarConfig(
                skinTone = AvatarSkinTone.DARK,
                presentation = AvatarPresentation.FEMININE,
                hairStyle = AvatarHairStyle.CURLY,
                glassesStyle = AvatarGlassesStyle.ROUND,
                hatStyle = AvatarHatStyle.BEANIE,
            ),
            profileRepository.profile.value.avatar,
        )
        assertEquals(AppThemePreference.LIGHT, themeRepository.preference.value)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals("Settings saved", viewModel.uiState.value.message)
    }

    @Test
    fun saveSettings_blankFeatureFieldsRestoreDefaults() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(UserProfile())
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.updateDisplayName("Alex")
        viewModel.updateAgeYears("30")
        viewModel.updateDailyStepGoal("")
        viewModel.updateDailyWaterGoalMl("")
        viewModel.updateStrideLengthMeters("")
        viewModel.updateNightlySleepGoalMinutes("")
        viewModel.updateTypicalBedtime("")
        viewModel.updateTypicalWakeTime("")
        viewModel.updateSleepDetectionBufferMinutes("")
        viewModel.saveSettings()
        runCurrent()

        assertEquals(
            UserProfile(displayName = "Alex", ageYears = 30),
            profileRepository.profile.value,
        )
    }

    @Test
    fun saveSettings_requiresNameAndAge() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(UserProfile())
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.saveSettings()
        runCurrent()

        assertEquals(UserProfile(), profileRepository.profile.value)
        assertEquals("Name and age are mandatory", viewModel.uiState.value.message)
        assertEquals("Name is mandatory", viewModel.uiState.value.profile.displayNameError)
        assertEquals("Age is mandatory", viewModel.uiState.value.profile.ageYearsError)
    }

    @Test
    fun saveSettings_rejectsInvalidAge() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(UserProfile())
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.updateDisplayName("Alex")
        viewModel.updateAgeYears("999")
        viewModel.saveSettings()
        runCurrent()

        assertEquals(UserProfile(), profileRepository.profile.value)
        assertEquals("Enter a valid age", viewModel.uiState.value.message)
        assertEquals("Enter a valid age", viewModel.uiState.value.profile.ageYearsError)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun saveSettings_rejectsInvalidSleepWindowTime() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(UserProfile())
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.updateDisplayName("Alex")
        viewModel.updateAgeYears("30")
        viewModel.updateTypicalBedtime("25:00")
        viewModel.saveSettings()
        runCurrent()

        assertEquals(UserProfile(), profileRepository.profile.value)
        assertEquals("Enter bedtime as HH:mm", viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun resetSettings_clearsOptionalFieldsAndRestoresDefaults() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Runner",
                weightKg = 80.0,
                heightCm = 182,
                strideLengthMeters = 0.9,
                preferredUnits = "imperial",
                dailyStepGoal = 15000,
                dailyWaterGoalMl = 3000,
                nightlySleepGoalMinutes = 510,
                typicalBedtimeMinutes = 1370,
                typicalWakeTimeMinutes = 410,
                sleepDetectionBufferMinutes = 55,
                ageYears = 34,
                gender = "Non-binary",
                avatar = AvatarConfig(
                    skinTone = AvatarSkinTone.DARK,
                    presentation = AvatarPresentation.FEMININE,
                    hairStyle = AvatarHairStyle.LONG,
                    glassesStyle = AvatarGlassesStyle.ROUND,
                    hatStyle = AvatarHatStyle.SUN_HAT,
                ),
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.DARK)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.resetSettings()
        runCurrent()

        val saved = profileRepository.profile.value
        assertNull(saved.displayName)
        assertNull(saved.weightKg)
        assertNull(saved.heightCm)
        assertNull(saved.ageYears)
        assertNull(saved.gender)
        assertEquals(0.78, saved.strideLengthMeters, 0.0)
        assertEquals("metric", saved.preferredUnits)
        assertEquals(10000, saved.dailyStepGoal)
        assertEquals(2000, saved.dailyWaterGoalMl)
        assertEquals(480, saved.nightlySleepGoalMinutes)
        assertEquals(1320, saved.typicalBedtimeMinutes)
        assertEquals(420, saved.typicalWakeTimeMinutes)
        assertEquals(30, saved.sleepDetectionBufferMinutes)
        assertEquals(AvatarConfig(), saved.avatar)
        assertEquals(AppThemePreference.SYSTEM, themeRepository.preference.value)
        assertTrue(viewModel.uiState.value.message?.contains("reset", ignoreCase = true) == true)
    }

    @Test
    fun avatarDraft_changesStayEphemeralUntilApplied() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Alex",
                ageYears = 30,
                avatar = AvatarConfig(hairStyle = AvatarHairStyle.SHORT),
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.openAvatarEditor()
        viewModel.updateAvatarHairStyle(AvatarHairStyle.BUN)
        viewModel.updateAvatarHatStyle(AvatarHatStyle.BEANIE)

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(AvatarHairStyle.SHORT, viewModel.uiState.value.profile.avatar.hairStyle)
        assertEquals(AvatarHairStyle.BUN, viewModel.uiState.value.avatarEditor.draft.hairStyle)
        assertEquals(AvatarHatStyle.NONE, profileRepository.profile.value.avatar.hatStyle)
    }

    @Test
    fun dismissAvatarEditor_discardsDraftChanges() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Alex",
                ageYears = 30,
                avatar = AvatarConfig(glassesStyle = AvatarGlassesStyle.NONE),
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.openAvatarEditor()
        viewModel.updateAvatarGlassesStyle(AvatarGlassesStyle.BOLD)
        viewModel.dismissAvatarEditor()

        assertFalse(viewModel.uiState.value.avatarEditor.isVisible)
        assertEquals(AvatarGlassesStyle.NONE, viewModel.uiState.value.avatarEditor.draft.glassesStyle)
        assertEquals(AvatarGlassesStyle.NONE, viewModel.uiState.value.profile.avatar.glassesStyle)
    }

    @Test
    fun resetAvatarDraftToSaved_restoresSavedAvatarWithoutDirtyFlag() = runTest(dispatcher) {
        val savedAvatar = AvatarConfig(
            skinTone = AvatarSkinTone.MEDIUM_DARK,
            presentation = AvatarPresentation.MASCULINE,
            hairStyle = AvatarHairStyle.SPIKY,
            glassesStyle = AvatarGlassesStyle.RECTANGULAR,
            hatStyle = AvatarHatStyle.CAP,
        )
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Alex",
                ageYears = 30,
                avatar = savedAvatar,
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.openAvatarEditor()
        viewModel.updateAvatarHairStyle(AvatarHairStyle.BUN)
        viewModel.updateAvatarHatStyle(AvatarHatStyle.SUN_HAT)
        viewModel.resetAvatarDraftToSaved()

        assertEquals(savedAvatar, viewModel.uiState.value.avatarEditor.draft)
        assertEquals(savedAvatar, viewModel.uiState.value.profile.avatar)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun applyAvatarDraft_withoutChangesKeepsDirtyFlagFalse() = runTest(dispatcher) {
        val savedAvatar = AvatarConfig(
            skinTone = AvatarSkinTone.MEDIUM_LIGHT,
            presentation = AvatarPresentation.NEUTRAL,
            hairStyle = AvatarHairStyle.MEDIUM,
            glassesStyle = AvatarGlassesStyle.ROUND,
            hatStyle = AvatarHatStyle.NONE,
        )
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Alex",
                ageYears = 30,
                avatar = savedAvatar,
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.openAvatarEditor()
        viewModel.applyAvatarDraft()

        assertFalse(viewModel.uiState.value.avatarEditor.isVisible)
        assertEquals(savedAvatar, viewModel.uiState.value.profile.avatar)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun applyAvatarDraft_updatesProfileStateAndMarksSettingsDirty() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository(
            UserProfile(
                displayName = "Alex",
                ageYears = 30,
                avatar = AvatarConfig(),
            )
        )
        val themeRepository = FakeAppThemeRepository(AppThemePreference.SYSTEM)
        val viewModel = SettingsViewModel(profileRepository, themeRepository)
        runCurrent()

        viewModel.openAvatarEditor()
        viewModel.updateAvatarSkinTone(AvatarSkinTone.MEDIUM_LIGHT)
        viewModel.updateAvatarPresentation(AvatarPresentation.FEMININE)
        viewModel.applyAvatarDraft()

        assertFalse(viewModel.uiState.value.avatarEditor.isVisible)
        assertEquals(AvatarSkinTone.MEDIUM_LIGHT, viewModel.uiState.value.profile.avatar.skinTone)
        assertEquals(AvatarPresentation.FEMININE, viewModel.uiState.value.profile.avatar.presentation)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(AvatarConfig(), profileRepository.profile.value.avatar)
    }
}

private class FakeUserProfileRepository(initial: UserProfile) : UserProfileRepository {
    val profile = MutableStateFlow(initial)

    override fun observeUserProfile(): Flow<UserProfile> = profile

    override fun getUserProfileBlocking(): UserProfile = profile.value

    override suspend fun updateUserProfile(profile: UserProfile) {
        this.profile.value = profile
    }
}

private class FakeAppThemeRepository(initial: AppThemePreference) : AppThemeRepository {
    val preference = MutableStateFlow(initial)

    override fun observeThemePreference(): Flow<AppThemePreference> = preference

    override suspend fun updateThemePreference(preference: AppThemePreference) {
        this.preference.value = preference
    }
}
