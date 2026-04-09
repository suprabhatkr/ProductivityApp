package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.model.UserProfile
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
    fun init_loadsProfileIntoEditableState() = runTest(dispatcher) {
        val repo = FakeUserProfileRepository(
            UserProfile(
                displayName = "Suprabhat",
                weightKg = 72.5,
                heightCm = 178,
                strideLengthMeters = 0.82,
                preferredUnits = "imperial",
                dailyStepGoal = 12000,
                dailyWaterGoalMl = 2600,
            )
        )

        val vm = SettingsViewModel(repo)
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Suprabhat", state.displayName)
        assertEquals("72.5", state.weightKg)
        assertEquals("178", state.heightCm)
        assertEquals("0.82", state.strideLengthMeters)
        assertEquals("imperial", state.preferredUnits)
        assertEquals("12000", state.dailyStepGoal)
        assertEquals("2600", state.dailyWaterGoalMl)
    }

    @Test
    fun saveProfile_updatesRepositoryAndClearsDirtyFlag() = runTest(dispatcher) {
        val repo = FakeUserProfileRepository(UserProfile())
        val vm = SettingsViewModel(repo)
        runCurrent()

        vm.updateDisplayName("Runner")
        vm.updateWeightKg("70.0")
        vm.updateHeightCm("175")
        vm.updateStrideLengthMeters("0.81")
        vm.updatePreferredUnits("metric")
        vm.updateDailyStepGoal("9000")
        vm.updateDailyWaterGoalMl("2300")
        vm.saveProfile()
        runCurrent()

        assertEquals("Runner", repo.profile.value.displayName)
        assertEquals(70.0, repo.profile.value.weightKg)
        assertEquals(175, repo.profile.value.heightCm)
        assertEquals(0.81, repo.profile.value.strideLengthMeters, 0.0)
        assertEquals("metric", repo.profile.value.preferredUnits)
        assertEquals(9000, repo.profile.value.dailyStepGoal)
        assertEquals(2300, repo.profile.value.dailyWaterGoalMl)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        assertEquals("Settings saved", vm.uiState.value.message)
    }

    @Test
    fun resetProfile_clearsOptionalFieldsAndRestoresDefaults() = runTest(dispatcher) {
        val repo = FakeUserProfileRepository(
            UserProfile(
                displayName = "Runner",
                weightKg = 80.0,
                heightCm = 182,
                strideLengthMeters = 0.9,
                preferredUnits = "imperial",
                dailyStepGoal = 15000,
                dailyWaterGoalMl = 3000,
            )
        )
        val vm = SettingsViewModel(repo)
        runCurrent()

        vm.resetProfile()
        runCurrent()

        val saved = repo.profile.value
        assertNull(saved.displayName)
        assertNull(saved.weightKg)
        assertNull(saved.heightCm)
        assertEquals(0.78, saved.strideLengthMeters, 0.0)
        assertEquals("metric", saved.preferredUnits)
        assertEquals(10000, saved.dailyStepGoal)
        assertEquals(2000, saved.dailyWaterGoalMl)
        assertTrue(vm.uiState.value.message?.contains("reset", ignoreCase = true) == true)
    }
}

private class FakeUserProfileRepository(initial: UserProfile) : UserProfileRepository {
    val profile = MutableStateFlow(initial)

    override fun observeUserProfile(): Flow<UserProfile> = profile

    override suspend fun updateUserProfile(profile: UserProfile) {
        this.profile.value = profile
    }
}

