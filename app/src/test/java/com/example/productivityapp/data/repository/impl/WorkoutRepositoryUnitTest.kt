package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.entities.WorkoutEntity
import com.example.productivityapp.data.model.WorkoutType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomWorkoutRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomWorkoutRepository(db.workoutDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startWorkout_persistsEntityAndReturnsId() = runTest {
        val workout = WorkoutEntity(
            workoutType = WorkoutType.YOGA.storageValue,
            startTime = 1_700_000_000_000L,
            endTime = null,
            durationSec = 0L,
        )

        val id = repository.startWorkout(workout)

        val stored = repository.getWorkoutById(id)
        assertNotNull(stored)
        assertEquals(WorkoutType.YOGA.storageValue, stored?.workoutType)
        assertEquals(id, stored?.id)
    }

    @Test
    fun updateWorkout_finishesActiveWorkout() = runTest {
        val id = repository.startWorkout(
            WorkoutEntity(
                workoutType = WorkoutType.PUSHUPS.storageValue,
                startTime = 1_700_000_000_000L,
                endTime = null,
                durationSec = 0L,
            )
        )
        val active = repository.getActiveWorkout() ?: error("Expected active workout")

        repository.updateWorkout(
            active.copy(
                endTime = active.startTime + 900_000L,
                durationSec = 900L,
            )
        )

        val stored = repository.getWorkoutById(id)
        assertEquals(900L, stored?.durationSec)
        assertNull(repository.getActiveWorkout())
    }
}
