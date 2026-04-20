package com.example.productivityapp.ui.sleep

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepScreenHelpersTest {

    @Test
    fun nextWakeTriggerMillis_usesNextDayWhenTimeHasPassed() {
        val now = ZonedDateTime.parse("2026-04-20T23:30:00+05:30[Asia/Kolkata]")

        val trigger = nextWakeTriggerMillis("07:00", now)

        val actual = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(trigger), ZoneId.of("Asia/Kolkata"))
        assertEquals(ZonedDateTime.parse("2026-04-21T07:00:00+05:30[Asia/Kolkata]"), actual)
    }

    @Test
    fun nextWakeTriggerMillis_keepsSameDayWhenTimeIsAhead() {
        val now = ZonedDateTime.parse("2026-04-20T05:30:00+05:30[Asia/Kolkata]")

        val trigger = nextWakeTriggerMillis("07:00", now)

        val actual = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(trigger), ZoneId.of("Asia/Kolkata"))
        assertEquals(ZonedDateTime.parse("2026-04-20T07:00:00+05:30[Asia/Kolkata]"), actual)
    }
}
