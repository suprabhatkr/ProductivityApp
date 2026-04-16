package com.example.productivityapp.data.sleep

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.model.UserProfile
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class AndroidSleepSignalProviderTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        AndroidSleepSignalProvider.isInteractiveProvider = { true }
        AndroidSleepSignalProvider.batteryStatusProvider = { -1 }
    }

    @Test
    fun collect_returnsSignalWhenDeviceIsChargingAndSleeping() {
        AndroidSleepSignalProvider.isInteractiveProvider = { false }
        AndroidSleepSignalProvider.batteryStatusProvider = { android.os.BatteryManager.BATTERY_STATUS_CHARGING }

        val profile = UserProfile(
            typicalBedtimeMinutes = 22 * 60,
            typicalWakeTimeMinutes = 7 * 60,
            sleepDetectionBufferMinutes = 30,
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 10),
            java.time.LocalTime.of(23, 30),
            ZoneId.of("Asia/Kolkata"),
        )

        val result = AndroidSleepSignalProvider.collect(context, profile, now)

        assertNotNull(result)
    }

    @Test
    fun collect_returnsNullWhenDeviceIsInteractive() {
        AndroidSleepSignalProvider.isInteractiveProvider = { true }
        AndroidSleepSignalProvider.batteryStatusProvider = { android.os.BatteryManager.BATTERY_STATUS_CHARGING }

        val profile = UserProfile()
        val now = ZonedDateTime.now(ZoneId.of("UTC"))

        assertNull(AndroidSleepSignalProvider.collect(context, profile, now))
    }

    @Test
    fun collect_returnsNullWhenDeviceIsNotCharging() {
        AndroidSleepSignalProvider.isInteractiveProvider = { false }
        AndroidSleepSignalProvider.batteryStatusProvider = { android.os.BatteryManager.BATTERY_STATUS_DISCHARGING }

        val profile = UserProfile()
        val now = ZonedDateTime.now(ZoneId.of("UTC"))

        assertNull(AndroidSleepSignalProvider.collect(context, profile, now))
    }
}
