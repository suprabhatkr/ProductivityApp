package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StepViewModelTest {
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
    fun observesProfileGoalAndStepProgress() = runTest(dispatcher) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val stepRepo = FakeStepRepository(
            StepEntity(
                date = today,
                steps = 4200,
                distanceMeters = 3200.0,
                calories = 180.0,
                source = "manual",
                lastUpdatedAt = 1L,
            )
        )
        val profileRepo = FakeStepUserProfileRepository(UserProfile(dailyStepGoal = 9000))

        val vm = StepViewModel(stepRepo, profileRepo)
        runCurrent()

        assertEquals(4200, vm.steps.value)
        assertEquals(9000, vm.dailyGoal.value)

        profileRepo.profile.value = UserProfile(dailyStepGoal = 12000)
        runCurrent()

        assertEquals(12000, vm.dailyGoal.value)
    }
}

private class FakeStepRepository(initial: StepEntity?) : StepRepository {
    private val entity = MutableStateFlow(initial)

    override fun observeStepsForDate(date: String): Flow<StepEntity?> = entity

    override suspend fun getStepsForDate(date: String): StepEntity? = entity.value

    override suspend fun getStepsForRange(startDate: String, endDate: String): List<StepEntity> {
        return entity.value?.let { listOf(it) }.orEmpty()
    }

    override fun observeStepsForRange(startDate: String, endDate: String): Flow<List<StepEntity>> {
        return entity.map { current -> current?.let { listOf(it) }.orEmpty() }
    }

    override suspend fun upsertSteps(step: StepEntity) {
        entity.value = step
    }

    override suspend fun incrementSteps(date: String, delta: Int, source: String) {
        val current = entity.value
        entity.value = if (current == null) {
            StepEntity(
                date = date,
                steps = delta,
                distanceMeters = 0.0,
                calories = 0.0,
                source = source,
                lastUpdatedAt = System.currentTimeMillis(),
            )
        } else {
            current.copy(
                steps = current.steps + delta,
                source = source,
                lastUpdatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun resetStepsForDate(date: String) {
        entity.value = null
    }

    override fun observeSamplesForDate(date: String): Flow<List<com.example.productivityapp.data.entities.StepSampleEntity>> {
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun getSamplesForDate(date: String): List<com.example.productivityapp.data.entities.StepSampleEntity> {
        return emptyList()
    }

    override suspend fun insertSample(sample: com.example.productivityapp.data.entities.StepSampleEntity) {
        Unit
    }
}

private class FakeStepUserProfileRepository(initial: UserProfile) : UserProfileRepository {
    val profile = MutableStateFlow(initial)

    override fun observeUserProfile(): Flow<UserProfile> = profile.map { it }

    override fun getUserProfileBlocking(): UserProfile = profile.value

    override suspend fun updateUserProfile(profile: UserProfile) {
        this.profile.value = profile
    }
}

