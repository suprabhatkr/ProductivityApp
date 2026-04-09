package com.example.productivityapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.repository.impl.RoomRunRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomRunRepository

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = RoomRunRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetRun() = runBlocking {
        val run = RunEntity(
            startTime = System.currentTimeMillis(),
            endTime = null,
            distanceMeters = 1000.0,
            durationSec = 600,
            avgSpeedMps = 1.666,
            calories = 80.0,
            polyline = "[]"
        )
        val id = repo.startRun(run)
        val fetched = repo.getRunById(id)
        assertNotNull(fetched)
        assertEquals(1000.0, fetched!!.distanceMeters, 0.001)
    }
}

