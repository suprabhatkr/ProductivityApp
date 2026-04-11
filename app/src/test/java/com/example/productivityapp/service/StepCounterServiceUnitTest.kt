package com.example.productivityapp.service

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.DatabaseProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
class StepCounterServiceUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseProvider.setTestInstance(db)
    }

    @After
    fun tearDown() {
        db.close()
        DatabaseProvider.setTestInstance(null)
    }

    @Test
    fun startStopLifecycle_andNotificationAction_areSafe() {
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        val service = controller.get()
        service.hasStepSensorOverride = false

        val startResult = service.onStartCommand(
            Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_START },
            0,
            0
        )

        assertEquals(android.app.Service.START_NOT_STICKY, startResult)
        assertFalse(service.isRegistrationActiveForTest())

        val notification = service.buildNotificationForTest("Step counter running")
        assertNotNull(notification)
        assertEquals(1, notification.actions.size)
        assertEquals("Stop", notification.actions.first().title)

        service.onStartCommand(
            Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_STOP },
            0,
            0
        )
        assertFalse(service.isRegistrationActiveForTest())
    }

    @Test
    fun permissionDenied_doesNotCrashAndShowsFallbackNotification() {
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        val service = controller.get()
        service.hasStepSensorOverride = true
        service.registerSensorUpdatesOverride = { throw SecurityException("permission denied") }

        val startResult = service.onStartCommand(
            Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_START },
            0,
            0
        )

        assertEquals(android.app.Service.START_NOT_STICKY, startResult)
        assertFalse(service.isRegistrationActiveForTest())
        assertTrue(service.buildNotificationForTest("Activity recognition permission required").actions.isNotEmpty())
    }

    @Test
    fun handleStepCounterReading_batchesAndFlushesToRepository() = runBlocking {
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        val service = controller.get()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        service.currentDateProvider = { today }

        service.handleStepCounterReading(1000L) // baseline
        service.handleStepCounterReading(1005L) // pending 5, no flush yet
        Thread.sleep(100)
        assertEquals(null, db.stepDao().getByDate(today))

        service.handleStepCounterReading(1010L) // pending reaches 10, flush
        Thread.sleep(200)
        assertEquals(10, db.stepDao().getByDate(today)?.steps)

        service.handleStepCounterReading(1012L) // pending 2
        service.onStartCommand(
            Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_STOP },
            0,
            0
        )
        Thread.sleep(200)
        assertEquals(12, db.stepDao().getByDate(today)?.steps)
    }

    @Test
    fun dateRollover_keepsFirstNewDayDelta() = runBlocking {
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        val service = controller.get()
        val day1 = "2026-04-09"
        val day2 = "2026-04-10"

        service.currentDateProvider = { day1 }
        service.handleStepCounterReading(2000L)
        service.handleStepCounterReading(2010L)
        Thread.sleep(200)
        assertEquals(10, db.stepDao().getByDate(day1)?.steps)

        service.currentDateProvider = { day2 }
        service.handleStepCounterReading(2013L)
        service.onStartCommand(
            Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_STOP },
            0,
            0
        )
        Thread.sleep(200)
        assertEquals(3, db.stepDao().getByDate(day2)?.steps)
    }
}


