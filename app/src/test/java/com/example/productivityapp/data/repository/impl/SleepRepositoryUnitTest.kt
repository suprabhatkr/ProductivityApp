package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class SleepRepositoryUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomSleepRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomSleepRepository(db.sleepDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertSleepPlaceholder() = runTest {
        val now = System.currentTimeMillis()
        val sleep = com.example.productivityapp.data.entities.SleepEntity(
            date = "2026-04-07",
            startTimestamp = now - 3600_000,
            endTimestamp = now,
            durationSec = 3600,
            sleepQuality = 4,
            notes = "test"
        )
        val id = repo.startSleep(sleep)
        val sessions = repo.observeSleepForDate("2026-04-07").first()
        assertEquals(1, sessions.size)
    }
}



