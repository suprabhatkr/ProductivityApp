package com.example.productivityapp.data

import android.content.Context

class UiStateStore(private val context: Context) {
    private val prefs by lazy { context.applicationContext.getSharedPreferences("ui_state_prefs", Context.MODE_PRIVATE) }

    fun isStepUiRunning(): Boolean = prefs.getBoolean("step_ui_running", false)
    fun setStepUiRunning(running: Boolean) {
        prefs.edit().putBoolean("step_ui_running", running).apply()
    }

    fun isRunUiRunning(): Boolean = prefs.getBoolean("run_ui_running", false)
    fun setRunUiRunning(running: Boolean) {
        prefs.edit().putBoolean("run_ui_running", running).apply()
    }
}

