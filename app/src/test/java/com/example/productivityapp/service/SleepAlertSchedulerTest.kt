package com.example.productivityapp.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class SleepAlertSchedulerTest {

    @Test
    fun calculateDelayMillis_neverReturnsNegative() {
        val now = ZonedDateTime.parse("2026-04-15T22:00:00Z")
        val past = now.minusMinutes(10).toInstant().toEpochMilli()
        val future = now.plusMinutes(20).toInstant().toEpochMilli()

        assertEquals(0L, SleepAlertScheduler.calculateDelayMillis(now, past))
        assertEquals(20L * 60L * 1000L, SleepAlertScheduler.calculateDelayMillis(now, future))
    }
}
