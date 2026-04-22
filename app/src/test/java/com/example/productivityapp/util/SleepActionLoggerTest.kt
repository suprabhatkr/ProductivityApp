package com.example.productivityapp.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SleepActionLoggerTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.resolve("sleep_action.log").delete()
        SleepActionLogger.initialize(context)
    }

    @Test
    fun logEvent_writesToFile() {
        SleepActionLogger.logEvent("test_event", mapOf("mode" to "manual"))

        val logFile = context.filesDir.resolve("sleep_action.log")
        val contents = logFile.readText()
        assertTrue(contents.contains("test_event"))
        assertTrue(contents.contains("mode=manual"))
    }
}
