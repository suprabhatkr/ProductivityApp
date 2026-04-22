package com.example.productivityapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedules daily midnight reset work after reboot. */
class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            MidnightResetWorker.schedule(appContext)
            SleepMaintenanceWorker.schedule(appContext)
        }
    }
}
