package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class RunRepositoryUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomRunRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomRunRepository(db.runDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertRunPlaceholder() = runTest {
        // Simple smoke test: create a run entity and insert via DAO/repository
        val now = System.currentTimeMillis()
        val runEntity = com.example.productivityapp.data.entities.RunEntity(
            startTime = now,
            endTime = now + 1000,
            distanceMeters = 500.0,
            durationSec = 1,
            avgSpeedMps = 0.5,
            calories = 10.0,
            polyline = ""
        )
        val id = repo.startRun(runEntity)
        val fetched = repo.getRunById(id)
        assertEquals(id, fetched?.id)
    }
}


