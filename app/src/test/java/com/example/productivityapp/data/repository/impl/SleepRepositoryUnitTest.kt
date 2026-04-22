package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepReviewState
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            notes = "test",
            detectionSource = SleepDetectionSource.AUTO.storageValue,
            confidenceScore = 0.91,
            inferredStartTimestamp = now - 3600_000,
            inferredEndTimestamp = now,
            reviewState = SleepReviewState.PROVISIONAL.storageValue,
            tagsCsv = "overnight",
        )
        val id = repo.startSleep(sleep)
        val sessions = repo.observeSleepForDate("2026-04-07").first()
        assertEquals(1, sessions.size)
        assertEquals(SleepDetectionSource.AUTO.storageValue, sessions.first().detectionSource)
        assertEquals(SleepReviewState.PROVISIONAL.storageValue, sessions.first().reviewState)
        assertEquals("overnight", sessions.first().tagsCsv)
        assert(id > 0L)
    }

    @Test
    fun activeSessionAndWeeklyRangeQueries_work() = runTest {
        val today = LocalDate.now()
        val active = com.example.productivityapp.data.entities.SleepEntity(
            date = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
            startTimestamp = 1_000L,
            endTimestamp = 0L,
            durationSec = 0L,
            sleepQuality = null,
            notes = null,
            detectionSource = SleepDetectionSource.MANUAL.storageValue,
            confidenceScore = 1.0,
            reviewState = SleepReviewState.CONFIRMED.storageValue,
        )
        val old = com.example.productivityapp.data.entities.SleepEntity(
            date = today.minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
            startTimestamp = 2_000L,
            endTimestamp = 4_000L,
            durationSec = 7200L,
            sleepQuality = 5,
            notes = "good",
            detectionSource = SleepDetectionSource.NAP.storageValue,
            confidenceScore = 0.9,
            reviewState = SleepReviewState.CONFIRMED.storageValue,
        )

        repo.startSleep(old)
        repo.startSleep(active)

        assertNotNull(repo.getActiveSleepSession())
        val weekly = repo.observeSleepForRange(
            today.minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE),
            today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).first()
        assertEquals(2, weekly.size)
    }
}
