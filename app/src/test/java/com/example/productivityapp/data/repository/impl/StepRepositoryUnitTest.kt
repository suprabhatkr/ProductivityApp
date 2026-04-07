package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.entities.StepEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class StepRepositoryUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomStepRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomStepRepository(db.stepDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testIncrementAndReset() = runTest {
        val today = "2026-04-07"

        // Initially no entry
        var pre = repo.getStepsForDate(today)
        assertNull(pre)

        // Increment by 150
        repo.incrementSteps(today, 150, "test")

        val mid = repo.getStepsForDate(today)
        assertNotNull(mid)
        assertEquals(150, mid!!.steps)

        // Increment again
        repo.incrementSteps(today, 50, "sensor")
        val updated = repo.getStepsForDate(today)
        assertNotNull(updated)
        assertEquals(200, updated!!.steps)

        // Reset
        repo.resetStepsForDate(today)
        val afterReset = repo.getStepsForDate(today)
        assertNull(afterReset)
    }
}

