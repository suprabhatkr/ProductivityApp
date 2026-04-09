package com.example.productivityapp.datastore

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPreferencesDataStoreIncrementAndObserve() = runTest {
        val file = File(tempFolder.root, "user_prefs.preferences_pb")

        val dsJob = kotlinx.coroutines.Job()
        val dsScope = CoroutineScope(dsJob + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dsScope,
            produceFile = { file }
        )

        val date = "2026-04-09"
        val waterKey = intPreferencesKey("water_" + date)

        // initial value should be 0 (absent)
        val initial = dataStore.data.first()[waterKey] ?: 0
        assertEquals(0, initial)

        // increment by writing a value
        dataStore.edit { prefs ->
            val current = prefs[waterKey] ?: 0
            prefs[waterKey] = current + 250
        }

        val after = dataStore.data.first()[waterKey] ?: 0
        assertEquals(250, after)

        // bump profile_version key and verify emission works (sanity)
        val versionKey = intPreferencesKey("profile_version")
        val v0 = dataStore.data.first()[versionKey] ?: 0
        dataStore.edit { prefs -> prefs[versionKey] = (prefs[versionKey] ?: 0) + 1 }
        val v1 = dataStore.data.first()[versionKey] ?: 0
        assertEquals(v0 + 1, v1)

        // cancel background scope used by DataStore to avoid leaking coroutines in tests
        dsJob.cancel()
    }
}




