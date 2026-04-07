package com.example.productivityapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Minimal BootCompleteReceiver stub to re-schedule midnight reset worker on device boot. */
class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Re-schedule midnight reset WorkManager job. Implementation will be added in a follow-up PR.
    }
}

