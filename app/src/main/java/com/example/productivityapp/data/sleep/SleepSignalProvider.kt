package com.example.productivityapp.data.sleep

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.annotation.VisibleForTesting
import com.example.productivityapp.data.model.UserProfile
import android.util.Log
import java.time.ZonedDateTime

interface SleepSignalProvider {
    fun collect(context: Context, profile: UserProfile, now: ZonedDateTime): SleepSignalSnapshot?
}

object AndroidSleepSignalProvider : SleepSignalProvider {
    @VisibleForTesting
    internal var isInteractiveProvider: (Context) -> Boolean = { context ->
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isInteractive ?: true
    }

    @VisibleForTesting
    internal var batteryStatusProvider: (Context) -> Int = { context ->
        val statusIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        statusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    }

    override fun collect(context: Context, profile: UserProfile, now: ZonedDateTime): SleepSignalSnapshot? {
        return try {
            if (isInteractiveProvider(context)) return null
            if (!isCharging(context)) return null

            val minuteOfDay = now.toLocalTime().hour * 60 + now.toLocalTime().minute
            if (!isWithinSleepWindow(minuteOfDay, profile.typicalBedtimeMinutes, profile.typicalWakeTimeMinutes)) {
                return null
            }

            val conservativeIdleMinutes = maxOf(profile.sleepDetectionBufferMinutes * 6, 180)
            val nowEpochMs = now.toInstant().toEpochMilli()
            SleepSignalSnapshot(
                idleMinutes = conservativeIdleMinutes,
                lastInteractionMinutesAgo = conservativeIdleMinutes,
                wakeInteractionMinutesAgo = null,
                isCharging = true,
                hasForegroundActivity = false,
                nowEpochMs = nowEpochMs,
            )
        } catch (securityException: SecurityException) {
            Log.w("AndroidSleepSignalProvider", "Sleep signal access unavailable; skipping auto-detection", securityException)
            null
        }
    }

    private fun isCharging(context: Context): Boolean {
        val status = batteryStatusProvider(context)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isWithinSleepWindow(minuteOfDay: Int, bedtimeMinutes: Int, wakeMinutes: Int): Boolean {
        return if (bedtimeMinutes <= wakeMinutes) {
            minuteOfDay in bedtimeMinutes..wakeMinutes
        } else {
            minuteOfDay >= bedtimeMinutes || minuteOfDay <= wakeMinutes
        }
    }
}
