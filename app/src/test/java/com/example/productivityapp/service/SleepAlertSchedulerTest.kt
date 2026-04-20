package com.example.productivityapp.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class SleepAlertSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

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
}
