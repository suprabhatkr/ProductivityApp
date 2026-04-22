package com.example.productivityapp.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object SleepAlertScheduler {
    const val UNIQUE_NAP_REMINDER_WORK = "sleep_nap_reminder_work"
    const val UNIQUE_WAKE_ALARM_WORK = "sleep_wake_alarm_work"
    const val EXTRA_ALERT_TYPE = "sleep_alert_type"
    const val EXTRA_ALERT_TITLE = "sleep_alert_title"
    const val EXTRA_ALERT_MESSAGE = "sleep_alert_message"
    const val EXTRA_NOTIFICATION_ID = "sleep_alert_notification_id"
    const val ALERT_TYPE_NAP_REMINDER = "nap_reminder"
    const val ALERT_TYPE_WAKE_ALARM = "wake_alarm"

    private const val NAP_REMINDER_DELAY_MINUTES = 15L

    @VisibleForTesting
    internal var nowProvider: () -> ZonedDateTime = { ZonedDateTime.now() }

    @VisibleForTesting
    internal var exactAlarmCapabilityProvider: (Context) -> Boolean = { context ->
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> true
            else -> {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() == true
            }
        }
    }

    fun scheduleNapReminder(context: Context, delayMinutes: Long = NAP_REMINDER_DELAY_MINUTES) {
        cancelNapReminder(context)
        val delay = Duration.ofMinutes(delayMinutes.coerceAtLeast(1L))
        val request = OneTimeWorkRequestBuilder<SleepReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                SleepReminderWorker.workData(
                    type = ALERT_TYPE_NAP_REMINDER,
                    title = "Nap reminder",
                    message = "Check whether you're awake or continue your nap.",
                    notificationId = SleepReminderWorker.NAP_NOTIFICATION_ID,
                )
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_NAP_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelNapReminder(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_NAP_REMINDER_WORK)
    }

    fun scheduleWakeAlarm(context: Context, triggerAtMillis: Long, exact: Boolean) {
        cancelWakeAlarm(context)
        if (exact && canScheduleExactAlarms(context)) {
            scheduleExactWakeAlarm(context, triggerAtMillis)
            return
        }

        val delayMs = (triggerAtMillis - nowProvider().toInstant().toEpochMilli()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<SleepReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                SleepReminderWorker.workData(
                    type = ALERT_TYPE_WAKE_ALARM,
                    title = "Wake-up alarm",
                    message = "It's time to wake up.",
                    notificationId = SleepReminderWorker.WAKE_NOTIFICATION_ID,
                )
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WAKE_ALARM_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun canScheduleExactAlarms(context: Context): Boolean = exactAlarmCapabilityProvider(context)

    fun hasWakeAlarmScheduled(context: Context): Boolean {
        return buildWakePendingIntent(context, PendingIntent.FLAG_NO_CREATE) != null
    }

    fun cancelWakeAlarm(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WAKE_ALARM_WORK)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        buildWakePendingIntent(context, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    @VisibleForTesting
    internal fun calculateDelayMillis(now: ZonedDateTime, triggerAtMillis: Long): Long {
        return (triggerAtMillis - now.toInstant().toEpochMilli()).coerceAtLeast(0L)
    }

    private fun scheduleExactWakeAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildWakePendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    private fun buildWakePendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALERT_TYPE, ALERT_TYPE_WAKE_ALARM)
            putExtra(EXTRA_ALERT_TITLE, "Wake-up alarm")
            putExtra(EXTRA_ALERT_MESSAGE, "It's time to wake up.")
            putExtra(EXTRA_NOTIFICATION_ID, SleepReminderWorker.WAKE_NOTIFICATION_ID)
        }
        return PendingIntent.getBroadcast(
            context,
            4010,
            intent,
            PendingIntent.FLAG_IMMUTABLE or flags,
        )
    }
}
