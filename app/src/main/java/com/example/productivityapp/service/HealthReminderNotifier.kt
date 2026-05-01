package com.example.productivityapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.productivityapp.MainActivity
import java.time.LocalTime

object HealthReminderNotifier {
    private const val WATER_CHANNEL_ID = "water_reminder_channel"
    private const val RUN_CHANNEL_ID = "run_reminder_channel"
    private const val STEP_CHANNEL_ID = "step_reminder_channel"
    private const val SLEEP_CHANNEL_ID = "sleep_reminder_channel"

    private const val WATER_FIRST_ID = 4101
    private const val WATER_INTERVAL_ID = 4102
    private const val RUN_HALF_ID = 4201
    private const val RUN_NINETY_ID = 4202
    private const val STEP_HALF_ID = 4301
    private const val STEP_NINETY_ID = 4302
    private const val STEP_EVENING_ID = 4303
    private const val SLEEP_BEDTIME_ID = 4401
    private const val SLEEP_NAP_ID = 4402
    private const val SLEEP_FOLLOW_UP_ID = 4403

    fun sendWaterFirstDrinkReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = WATER_CHANNEL_ID,
            channelName = "Water reminders",
            notificationId = WATER_FIRST_ID,
            copy = HealthReminderContent.pick(ReminderEvent.WATER_FIRST_DRINK, now = now),
        )
    }

    fun sendWaterIdleReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = WATER_CHANNEL_ID,
            channelName = "Water reminders",
            notificationId = WATER_INTERVAL_ID,
            copy = HealthReminderContent.pick(ReminderEvent.WATER_INTERVAL, now = now),
        )
    }

    fun sendRunHalfReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = RUN_CHANNEL_ID,
            channelName = "Run reminders",
            notificationId = RUN_HALF_ID,
            copy = HealthReminderContent.pick(ReminderEvent.RUN_HALF, now = now),
        )
    }

    fun sendRunNinetyReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = RUN_CHANNEL_ID,
            channelName = "Run reminders",
            notificationId = RUN_NINETY_ID,
            copy = HealthReminderContent.pick(ReminderEvent.RUN_NINETY, now = now),
        )
    }

    fun sendStepHalfReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = STEP_CHANNEL_ID,
            channelName = "Step reminders",
            notificationId = STEP_HALF_ID,
            copy = HealthReminderContent.pick(ReminderEvent.STEP_HALF, now = now),
        )
    }

    fun sendStepNinetyReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = STEP_CHANNEL_ID,
            channelName = "Step reminders",
            notificationId = STEP_NINETY_ID,
            copy = HealthReminderContent.pick(ReminderEvent.STEP_NINETY, now = now),
        )
    }

    fun sendStepEveningReminder(context: Context, remainingSteps: Int, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = STEP_CHANNEL_ID,
            channelName = "Step reminders",
            notificationId = STEP_EVENING_ID,
            copy = HealthReminderContent.pick(
                ReminderEvent.STEP_EVENING,
                now = now,
                remainingSteps = remainingSteps,
            ),
        )
    }

    fun sendSleepBedtimeReminder(context: Context, bedtimeLabel: String, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = SLEEP_CHANNEL_ID,
            channelName = "Sleep reminders",
            notificationId = SLEEP_BEDTIME_ID,
            copy = HealthReminderContent.pick(
                ReminderEvent.SLEEP_BEDTIME,
                now = now,
                bedtimeLabel = bedtimeLabel,
            ),
        )
    }

    fun sendNapStartedReminder(context: Context, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = SLEEP_CHANNEL_ID,
            channelName = "Sleep reminders",
            notificationId = SLEEP_NAP_ID,
            copy = HealthReminderContent.pick(ReminderEvent.SLEEP_NAP_STARTED, now = now),
        )
    }

    fun sendSleepFollowUpReminder(context: Context, durationLabel: String, now: LocalTime = LocalTime.now()) {
        notify(
            context = context,
            channelId = SLEEP_CHANNEL_ID,
            channelName = "Sleep reminders",
            notificationId = SLEEP_FOLLOW_UP_ID,
            copy = HealthReminderContent.pick(
                ReminderEvent.SLEEP_WAKE_FOLLOW_UP,
                now = now,
                durationLabel = durationLabel,
            ),
        )
    }

    private fun notify(
        context: Context,
        channelId: String,
        channelName: String,
        notificationId: Int,
        copy: ReminderCopy,
    ) {
        createChannel(context, channelId, channelName)
        val appContext = context.applicationContext
        val openIntent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(copy.title)
            .setContentText(copy.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createChannel(context: Context, channelId: String, channelName: String) {
        val notificationManager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }
}
