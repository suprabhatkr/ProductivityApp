package com.example.productivityapp.util

import android.content.Context
import android.util.Log
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

object SleepActionLogger {
    private const val LOG_FILE_NAME = "sleep_action.log"
    private const val TAG = "SleepActionLogger"

    private val appContextRef = AtomicReference<Context?>()

    fun initialize(context: Context) {
        val applicationContext = context.applicationContext
        if (appContextRef.get() !== applicationContext) {
            appContextRef.set(applicationContext)
            logEvent("logger_initialized", mapOf("file" to LOG_FILE_NAME))
        }
    }

    fun logEvent(event: String, details: Map<String, String> = emptyMap()) {
        writeLine("INFO", event, details, null)
    }

    fun logError(event: String, throwable: Throwable, details: Map<String, String> = emptyMap()) {
        writeLine("ERROR", event, details, throwable)
    }

    private fun writeLine(level: String, event: String, details: Map<String, String>, throwable: Throwable?) {
        val context = appContextRef.get() ?: return
        val logFile = context.filesDir.resolve(LOG_FILE_NAME)
        val detailText = details.entries
            .sortedBy { it.key }
            .joinToString(", ") { (key, value) -> "$key=$value" }
        val message = buildString {
            append(Instant.now().toString())
            append(" | ")
            append(level)
            append(" | ")
            append(event)
            if (detailText.isNotBlank()) {
                append(" | ")
                append(detailText)
            }
            if (throwable != null) {
                append(" | ")
                append(throwable.javaClass.simpleName)
                throwable.message?.takeIf { it.isNotBlank() }?.let {
                    append(": ")
                    append(it)
                }
                append("\n")
                append(Log.getStackTraceString(throwable))
            }
        }

        try {
            logFile.parentFile?.mkdirs()
            logFile.appendText(message)
            logFile.appendText("\n")
        } catch (writeError: Throwable) {
            Log.e(TAG, "Failed to write sleep log file", writeError)
        }
    }
}
