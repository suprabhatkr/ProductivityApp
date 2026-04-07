package com.example.productivityapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.repository.impl.RoomSleepRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomSleepRepository

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = RoomSleepRepository(db.sleepDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndObserveSleep() = runBlocking {
        val date = "2026-04-07"
        val sleep = SleepEntity(
            date = date,
            startTimestamp = System.currentTimeMillis() - 8 * 3600 * 1000,
            endTimestamp = System.currentTimeMillis(),
            durationSec = 8 * 3600,
            sleepQuality = 4,
            notes = "test"
        )
        val id = repo.startSleep(sleep)
        // Collect the DAO flow to verify the inserted session is observable
        val fetchedList = db.sleepDao().observeForDate(date).first()
        assertTrue(id > 0)
        assertTrue(fetchedList.isNotEmpty())
    }
}


