package com.example.productivityapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.productivityapp.data.repository.impl.RoomStepRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomStepRepository

    @Before
    fun createDb() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = RoomStepRepository(db.stepDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveSteps() = runBlocking {
        val date = "2026-04-07"
        repo.incrementSteps(date, 123, "test")
        val entity = repo.getStepsForDate(date)
        assertNotNull(entity)
        assertEquals(123, entity!!.steps)
    }
}

