package com.example.productivityapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepReviewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import java.time.ZonedDateTime

class SleepAlarmReceiver : BroadcastReceiver() {
    @VisibleForTesting
    internal var sleepRepositoryProvider: ((Context) -> com.example.productivityapp.data.repository.SleepRepository)? = null

    @VisibleForTesting
    internal var nowProvider: () -> ZonedDateTime = { ZonedDateTime.now() }

    @VisibleForTesting
    internal var cancelNapReminderAction: (Context) -> Unit = { SleepAlertScheduler.cancelNapReminder(it) }

    @VisibleForTesting
    internal var cancelWakeAlarmAction: (Context) -> Unit = { SleepAlertScheduler.cancelWakeAlarm(it) }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CONFIRM_WAKE -> confirmWake(context)
            ACTION_SNOOZE_NAP -> snoozeNap(context)
            ACTION_DISMISS_WAKE_ALARM -> dismissWakeAlarm(context)
            else -> Unit
        }
    }

    private fun confirmWake(context: Context) {
        runBlocking(Dispatchers.IO) {
            val repo = sleepRepositoryProvider?.invoke(context)
                ?: RepositoryProvider.provideSleepRepository(context)
            val active = repo.getActiveSleepSession() ?: return@runBlocking
            val now = nowProvider().toInstant().toEpochMilli()
            val confirmed = active.copy(
                endTimestamp = now,
                durationSec = ((now - active.startTimestamp).coerceAtLeast(0L) / 1000L),
                inferredEndTimestamp = now,
                reviewState = when (active.detectionSource) {
                    SleepDetectionSource.NAP.storageValue -> SleepReviewState.CONFIRMED.storageValue
                    else -> SleepReviewState.NEEDS_REVIEW.storageValue
                },
            )
            repo.stopSleep(confirmed)
            cancelNapReminderAction(context)
            cancelWakeAlarmAction(context)
        }
    }

    private fun snoozeNap(context: Context) {
        val request = OneTimeWorkRequestBuilder<SleepReminderWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(
                SleepReminderWorker.workData(
                    type = SleepAlertScheduler.ALERT_TYPE_NAP_REMINDER,
                    title = "Nap reminder",
                    message = "Check whether you're awake or continue your nap.",
                    notificationId = SleepReminderWorker.NAP_NOTIFICATION_ID,
                )
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            SleepAlertScheduler.UNIQUE_NAP_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun dismissWakeAlarm(context: Context) {
        cancelWakeAlarmAction(context)
    }

    companion object {
        const val ACTION_SNOOZE_NAP = "com.example.productivityapp.action.SNOOZE_NAP"
        const val ACTION_CONFIRM_WAKE = "com.example.productivityapp.action.CONFIRM_WAKE"
        const val ACTION_DISMISS_WAKE_ALARM = "com.example.productivityapp.action.DISMISS_WAKE_ALARM"
    }
}
