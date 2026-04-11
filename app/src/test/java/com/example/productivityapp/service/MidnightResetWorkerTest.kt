package com.example.productivityapp.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.DatabaseProvider
import com.example.productivityapp.data.entities.StepEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class MidnightResetWorkerTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseProvider.setTestInstance(db)

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executor { runnable -> runnable.run() })
            .build()
        runCatching { WorkManager.initialize(context, config) }
        workManager = WorkManager.getInstance(context)
        MidnightResetWorker.clearTestOverrides()
    }

    @After
    fun tearDown() {
        MidnightResetWorker.clearTestOverrides()
        DatabaseProvider.setTestInstance(null)
        db.close()
    }

    @Test
    fun calculateInitialDelay_alignsToNextMidnightInLocalTimezone() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 10),
            LocalTime.of(23, 59),
            ZoneId.of("Asia/Kolkata"),
        )

        val delay = MidnightResetWorker.calculateInitialDelay(now)

        assertEquals(60, delay.seconds)
    }

    @Test
    fun schedule_enqueuesNextRun() {
        val scheduleNow = ZonedDateTime.of(
            LocalDate.of(2026, 4, 10),
            LocalTime.of(23, 59),
            ZoneId.systemDefault(),
        )

        MidnightResetWorker.schedule(context, scheduleNow)
        val initialWork = workManager.getWorkInfosForUniqueWork(MidnightResetWorker.UNIQUE_WORK_NAME)
            .get()
            .first()
        assertEquals(WorkInfo.State.ENQUEUED, initialWork.state)
        assertTrue(initialWork.tags.contains(MidnightResetWorker.UNIQUE_WORK_NAME))
    }

    @Test
    fun bootReceiverSchedulesWorkerOnBootCompleted() {
        MidnightResetWorker.nowProvider = {
            ZonedDateTime.of(LocalDate.of(2026, 4, 10), LocalTime.NOON, ZoneId.systemDefault())
        }

        BootCompleteReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val workInfos = workManager.getWorkInfosForUniqueWork(MidnightResetWorker.UNIQUE_WORK_NAME).get()
        assertFalse(workInfos.isEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, workInfos.first().state)
    }

    @Test
    fun performResetOperations_clearsCurrentDayWaterAndSteps() = runTest {
        val date = LocalDate.of(2026, 4, 11)
        val dateString = date.toString()
        context.getSharedPreferences("water_data_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("entries_$dateString", "[{\"id\":1,\"amountMl\":250}]")
            .commit()
        db.stepDao().insert(
            StepEntity(
                date = dateString,
                steps = 321,
                distanceMeters = 10.0,
                calories = 5.0,
                source = "sensor",
                lastUpdatedAt = System.currentTimeMillis(),
            )
        )

        MidnightResetWorker.performResetOperations(context, date)

        assertNull(
            context.getSharedPreferences("water_data_prefs", Context.MODE_PRIVATE)
                .getString("entries_$dateString", null)
        )
        assertNull(db.stepDao().getByDate(dateString))
    }
}

