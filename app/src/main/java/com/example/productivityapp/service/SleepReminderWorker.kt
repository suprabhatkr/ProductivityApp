package com.example.productivityapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class SleepReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val notification = buildNotification(
            title = inputData.getString(SleepAlertScheduler.EXTRA_ALERT_TITLE) ?: "Sleep reminder",
            message = inputData.getString(SleepAlertScheduler.EXTRA_ALERT_MESSAGE) ?: "Open the app to review this sleep alert.",
            notificationId = inputData.getInt(SleepAlertScheduler.EXTRA_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID),
        )
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notification.second, notification.first)
        return Result.success()
    }

    private fun buildNotification(title: String, message: String, notificationId: Int): Pair<Notification, Int> {
        createChannel()
        val openIntent = Intent(applicationContext, com.example.productivityapp.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPending = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val snoozeIntent = Intent(applicationContext, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_SNOOZE_NAP
        }
        val snoozePending = PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 1,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val confirmIntent = Intent(applicationContext, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_CONFIRM_WAKE
        }
        val confirmPending = PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 2,
            confirmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val dismissIntent = Intent(applicationContext, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_DISMISS_WAKE_ALARM
        }
        val dismissPending = PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 3,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_view, "Open app", openPending)

        val notification = when (inputData.getString(SleepAlertScheduler.EXTRA_ALERT_TYPE)) {
            SleepAlertScheduler.ALERT_TYPE_NAP_REMINDER -> builder
                .addAction(android.R.drawable.ic_input_add, "I'm awake", confirmPending)
                .addAction(android.R.drawable.ic_menu_recent_history, "Continue nap", snoozePending)
                .build()

            SleepAlertScheduler.ALERT_TYPE_WAKE_ALARM -> builder
                .addAction(android.R.drawable.ic_delete, "Dismiss alarm", dismissPending)
                .build()

            else -> builder.build()
        }
        return notification to notificationId
    }

    private fun createChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sleep reminders",
            NotificationManager.IMPORTANCE_HIGH,
        )
        runCatching { nm.createNotificationChannel(channel) }
    }

    companion object {
        private const val CHANNEL_ID = "sleep_reminder_channel"
        const val DEFAULT_NOTIFICATION_ID = 3101
        const val NAP_NOTIFICATION_ID = 3102
        const val WAKE_NOTIFICATION_ID = 3103

        fun workData(
            type: String,
            title: String,
            message: String,
            notificationId: Int,
        ) = workDataOf(
            SleepAlertScheduler.EXTRA_ALERT_TYPE to type,
            SleepAlertScheduler.EXTRA_ALERT_TITLE to title,
            SleepAlertScheduler.EXTRA_ALERT_MESSAGE to message,
            SleepAlertScheduler.EXTRA_NOTIFICATION_ID to notificationId,
        )
    }
}
