package com.example.productivityapp.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDataStoreWaterIncrementAndReset_and_profileWriteRead() = runTest {
        val file = File(tempFolder.root, "user_prefs.preferences_pb")

        val dsJob = kotlinx.coroutines.Job()
        val dsScope = CoroutineScope(dsJob + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dsScope,
            produceFile = { file }
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val securePrefs = context.getSharedPreferences("test_secure_prefs", Context.MODE_PRIVATE)
        securePrefs.edit().clear().commit()

        val uds = UserDataStore(context, injectedDataStore = dataStore, injectedSecurePrefs = securePrefs)

        val date = "2026-04-11"
        // initial water should be 0
        val initial = uds.observeWaterForDate(date)
            .first()
        assertEquals(0, initial)

        // increment
        uds.incrementWater(date, 300)
        val after = uds.observeWaterForDate(date).first()
        assertEquals(300, after)

        // reset
        uds.resetWaterForDate(date)
        val afterReset = uds.observeWaterForDate(date).first()
        assertEquals(0, afterReset)

        // profile write/read
        val profile = com.example.productivityapp.data.model.UserProfile(
            displayName = "Test",
            weightKg = 72.5,
            heightCm = 180,
            strideLengthMeters = 0.75,
            preferredUnits = "metric",
            dailyStepGoal = 8000,
            dailyWaterGoalMl = 2500,
            nightlySleepGoalMinutes = 510,
            typicalBedtimeMinutes = 1375,
            typicalWakeTimeMinutes = 415,
            sleepDetectionBufferMinutes = 40,
        )

        uds.updateUserProfile(profile)

        val read = uds.getUserProfileBlocking()
        assertEquals("Test", read.displayName)
        assertEquals(72.5, read.weightKg!!, 0.0001)
        assertEquals(180, read.heightCm)
        assertEquals(0.75, read.strideLengthMeters, 0.0001)
        assertEquals("metric", read.preferredUnits)
        assertEquals(8000, read.dailyStepGoal)
        assertEquals(2500, read.dailyWaterGoalMl)
        assertEquals(510, read.nightlySleepGoalMinutes)
        assertEquals(1375, read.typicalBedtimeMinutes)
        assertEquals(415, read.typicalWakeTimeMinutes)
        assertEquals(40, read.sleepDetectionBufferMinutes)

        dsJob.cancel()
    }
}

