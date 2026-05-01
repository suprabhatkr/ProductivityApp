package com.example.productivityapp.service

import android.content.Context
import androidx.core.content.edit

class HealthReminderStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldNotifyWaterFirst(date: String): Boolean = prefs.getString(KEY_WATER_FIRST_DATE, null) != date

    fun markWaterFirst(date: String) {
        prefs.edit { putString(KEY_WATER_FIRST_DATE, date) }
    }

    fun shouldNotifyWaterIdle(date: String, latestEntryId: Long): Boolean {
        val rememberedDate = prefs.getString(KEY_WATER_IDLE_DATE, null)
        val rememberedEntryId = prefs.getLong(KEY_WATER_IDLE_ENTRY_ID, -1L)
        return rememberedDate != date || rememberedEntryId != latestEntryId
    }

    fun markWaterIdle(date: String, latestEntryId: Long) {
        prefs.edit {
            putString(KEY_WATER_IDLE_DATE, date)
            putLong(KEY_WATER_IDLE_ENTRY_ID, latestEntryId)
        }
    }

    fun shouldNotifyRunHalf(runId: Long): Boolean = prefs.getLong(KEY_RUN_HALF_ID, -1L) != runId

    fun markRunHalf(runId: Long) {
        prefs.edit { putLong(KEY_RUN_HALF_ID, runId) }
    }

    fun shouldNotifyRunNinety(runId: Long): Boolean = prefs.getLong(KEY_RUN_NINETY_ID, -1L) != runId

    fun markRunNinety(runId: Long) {
        prefs.edit { putLong(KEY_RUN_NINETY_ID, runId) }
    }

    fun shouldNotifyStepHalf(date: String): Boolean = prefs.getString(KEY_STEP_HALF_DATE, null) != date

    fun markStepHalf(date: String) {
        prefs.edit { putString(KEY_STEP_HALF_DATE, date) }
    }

    fun shouldNotifyStepNinety(date: String): Boolean = prefs.getString(KEY_STEP_NINETY_DATE, null) != date

    fun markStepNinety(date: String) {
        prefs.edit { putString(KEY_STEP_NINETY_DATE, date) }
    }

    fun shouldNotifyStepEvening(date: String): Boolean = prefs.getString(KEY_STEP_EVENING_DATE, null) != date

    fun markStepEvening(date: String) {
        prefs.edit { putString(KEY_STEP_EVENING_DATE, date) }
    }

    fun shouldNotifySleepBedtime(targetDate: String): Boolean =
        prefs.getString(KEY_SLEEP_BEDTIME_TARGET_DATE, null) != targetDate

    fun markSleepBedtime(targetDate: String) {
        prefs.edit { putString(KEY_SLEEP_BEDTIME_TARGET_DATE, targetDate) }
    }

    fun shouldNotifyNapStarted(sessionKey: String): Boolean =
        prefs.getString(KEY_SLEEP_NAP_SESSION_KEY, null) != sessionKey

    fun markNapStarted(sessionKey: String) {
        prefs.edit { putString(KEY_SLEEP_NAP_SESSION_KEY, sessionKey) }
    }

    fun shouldNotifySleepFollowUp(sessionKey: String): Boolean =
        prefs.getString(KEY_SLEEP_FOLLOW_UP_KEY, null) != sessionKey

    fun markSleepFollowUp(sessionKey: String) {
        prefs.edit { putString(KEY_SLEEP_FOLLOW_UP_KEY, sessionKey) }
    }

    private companion object {
        const val PREFS_NAME = "health_reminder_state"
        const val KEY_WATER_FIRST_DATE = "water_first_date"
        const val KEY_WATER_IDLE_DATE = "water_idle_date"
        const val KEY_WATER_IDLE_ENTRY_ID = "water_idle_entry_id"
        const val KEY_RUN_HALF_ID = "run_half_id"
        const val KEY_RUN_NINETY_ID = "run_ninety_id"
        const val KEY_STEP_HALF_DATE = "step_half_date"
        const val KEY_STEP_NINETY_DATE = "step_ninety_date"
        const val KEY_STEP_EVENING_DATE = "step_evening_date"
        const val KEY_SLEEP_BEDTIME_TARGET_DATE = "sleep_bedtime_target_date"
        const val KEY_SLEEP_NAP_SESSION_KEY = "sleep_nap_session_key"
        const val KEY_SLEEP_FOLLOW_UP_KEY = "sleep_follow_up_key"
    }
}
