package com.example.productivityapp.service

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZonedDateTime
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
class SleepAlertSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executor { runnable -> runnable.run() })
            .build()
        runCatching { WorkManager.initialize(context, config) }
        WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        SleepAlertScheduler.nowProvider = { ZonedDateTime.now() }
        SleepAlertScheduler.exactAlarmCapabilityProvider = { true }
    }

    @Test
    fun calculateDelayMillis_neverReturnsNegative() {
        val now = ZonedDateTime.parse("2026-04-15T22:00:00Z")
        val past = now.minusMinutes(10).toInstant().toEpochMilli()
        val future = now.plusMinutes(20).toInstant().toEpochMilli()

        assertEquals(0L, SleepAlertScheduler.calculateDelayMillis(now, past))
        assertEquals(20L * 60L * 1000L, SleepAlertScheduler.calculateDelayMillis(now, future))
    }

    @Test
    fun canScheduleExactAlarms_usesInjectedCapabilityProvider() {
        SleepAlertScheduler.exactAlarmCapabilityProvider = { false }
        assertFalse(SleepAlertScheduler.canScheduleExactAlarms(context))

        SleepAlertScheduler.exactAlarmCapabilityProvider = { true }
        assertTrue(SleepAlertScheduler.canScheduleExactAlarms(context))
    }

    @Test
    fun cancelWakeAlarm_isSafeWhenNoAlarmExists() {
        assertFalse(SleepAlertScheduler.hasWakeAlarmScheduled(context))
        SleepAlertScheduler.cancelWakeAlarm(context)
    }
}
