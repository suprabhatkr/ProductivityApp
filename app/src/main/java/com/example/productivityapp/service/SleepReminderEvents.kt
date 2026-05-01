package com.example.productivityapp.service

import android.content.Context
import com.example.productivityapp.data.entities.SleepEntity
import java.time.LocalTime

object SleepReminderEvents {
    fun notifyNapStarted(context: Context, sessionStartTimestamp: Long) {
        val appContext = context.applicationContext
        val stateStore = HealthReminderStateStore(appContext)
        val sessionKey = "nap:$sessionStartTimestamp"
        if (!stateStore.shouldNotifyNapStarted(sessionKey)) return
        HealthReminderNotifier.sendNapStartedReminder(appContext, LocalTime.now())
        stateStore.markNapStarted(sessionKey)
    }

    fun notifyWakeFollowUp(context: Context, session: SleepEntity) {
        if (session.endTimestamp <= 0L) return
        val appContext = context.applicationContext
        val stateStore = HealthReminderStateStore(appContext)
        val sessionKey = "${session.id}:${session.endTimestamp}"
        if (!stateStore.shouldNotifySleepFollowUp(sessionKey)) return
        HealthReminderNotifier.sendSleepFollowUpReminder(
            context = appContext,
            durationLabel = formatDuration(session.durationSec),
            now = LocalTime.now(),
        )
        stateStore.markSleepFollowUp(sessionKey)
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "%dh %02dm".format(hours, minutes) else "%dm".format(minutes)
    }
}
