package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.entities.WorkoutEntity
import com.example.productivityapp.data.model.WorkoutType
import com.example.productivityapp.data.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
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
    fun selectWorkoutType_updatesSelection() = runTest(dispatcher) {
        val viewModel = WorkoutViewModel(FakeWorkoutRepository())

        viewModel.selectWorkoutType(WorkoutType.OUTDOOR)

        assertEquals(WorkoutType.OUTDOOR, viewModel.selectedWorkoutType.value)
    }

    @Test
    fun startWorkout_createsActiveSessionForSelectedType() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository()
        val viewModel = WorkoutViewModel(repository)
        viewModel.selectWorkoutType(WorkoutType.INTENSE)

        viewModel.startWorkout()
        advanceUntilIdle()

        val activeWorkout = repository.activeFlow.value
        assertNotNull(activeWorkout)
        assertEquals(WorkoutType.INTENSE.storageValue, activeWorkout?.workoutType)
        assertEquals(WorkoutType.INTENSE, viewModel.selectedWorkoutType.value)
    }

    @Test
    fun endWorkout_finishesCurrentActiveSession() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(
            activeWorkout = WorkoutEntity(
                id = 9L,
                workoutType = WorkoutType.LIGHT.storageValue,
                startTime = System.currentTimeMillis() - 5_000L,
                endTime = null,
                durationSec = 0L,
            )
        )
        val viewModel = WorkoutViewModel(repository)
        advanceUntilIdle()

        viewModel.endWorkout()
        advanceUntilIdle()

        val finished = repository.workoutsFlow.value.first()
        assertTrue(finished.endTime != null)
        assertTrue(finished.durationSec >= 0L)
    }
}

private class FakeWorkoutRepository(
    activeWorkout: WorkoutEntity? = null,
) : WorkoutRepository {
    val workoutsFlow = MutableStateFlow(activeWorkout?.let { listOf(it) } ?: emptyList())
    val activeFlow = MutableStateFlow(activeWorkout)

    override fun observeWorkouts(): Flow<List<WorkoutEntity>> = workoutsFlow

    override fun observeLatestWorkout(): Flow<WorkoutEntity?> = flowOf(workoutsFlow.value.firstOrNull())

    override fun observeActiveWorkout(): Flow<WorkoutEntity?> = activeFlow

    override suspend fun getWorkoutById(id: Long): WorkoutEntity? = workoutsFlow.value.firstOrNull { it.id == id }

    override suspend fun getActiveWorkout(): WorkoutEntity? = activeFlow.value

    override suspend fun startWorkout(workout: WorkoutEntity): Long {
        val stored = workout.copy(id = if (workout.id == 0L) 1L else workout.id)
        workoutsFlow.value = listOf(stored) + workoutsFlow.value.filterNot { it.id == stored.id }
        activeFlow.value = stored
        return stored.id
    }

    override suspend fun updateWorkout(workout: WorkoutEntity) {
        workoutsFlow.value = workoutsFlow.value
            .map { existing -> if (existing.id == workout.id) workout else existing }
            .ifEmpty { listOf(workout) }
        activeFlow.value = workout.takeIf { it.endTime == null }
    }
}
