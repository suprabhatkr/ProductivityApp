package com.example.productivityapp.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.productivityapp.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.withContext

/**
 * Simple wrapper: encrypted SharedPreferences for sensitive user profile fields; Preferences DataStore
 * for per-day water counters and non-sensitive prefs.
 */
class UserDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.Job()),
        produceFile = { context.preferencesDataStoreFile("user_prefs") }
    )

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "user_profile_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Preferences keys (water stored per-date as int)
    private fun waterKeyForDate(date: String) = intPreferencesKey("water_" + date)
    private val PROFILE_NAME_KEY = "profile_name"
    private val PROFILE_WEIGHT_KEY = "profile_weight"
    private val PROFILE_HEIGHT_KEY = "profile_height"
    private val PROFILE_STRIDE_KEY = "profile_stride"
    private val PROFILE_UNITS_KEY = "profile_units"
    private val PROFILE_STEP_GOAL_KEY = "profile_step_goal"
    private val PROFILE_WATER_GOAL_KEY = "profile_water_goal"
    private val PROFILE_VERSION = intPreferencesKey("profile_version")
    fun observeWaterForDate(date: String): Flow<Int> {
        val key = waterKeyForDate(date)
        return dataStore.data.map { prefs -> prefs[key] ?: 0 }
    }

    suspend fun incrementWater(date: String, ml: Int) {
        val key = waterKeyForDate(date)
        dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            prefs[key] = current + ml
        }
    }

    suspend fun setWaterForDate(date: String, ml: Int) {
        val key = waterKeyForDate(date)
        dataStore.edit { prefs -> prefs[key] = ml }
    }

    suspend fun resetWaterForDate(date: String) {
        val key = waterKeyForDate(date)
        dataStore.edit { prefs -> prefs.remove(key) }
    }

    fun observeUserProfile(): Flow<UserProfile> = dataStore.data.map { prefs ->
        val name = securePrefs.getString(PROFILE_NAME_KEY, null)
        val weight = securePrefs.getString(PROFILE_WEIGHT_KEY, null)?.toDoubleOrNull()
        val height = securePrefs.getInt(PROFILE_HEIGHT_KEY, -1).let { if (it <= 0) null else it }
        val stride = securePrefs.getString(PROFILE_STRIDE_KEY, null)?.toDoubleOrNull() ?: 0.78
        val units = securePrefs.getString(PROFILE_UNITS_KEY, "metric") ?: "metric"
        val stepGoal = securePrefs.getInt(PROFILE_STEP_GOAL_KEY, 10000)
        val waterGoal = securePrefs.getInt(PROFILE_WATER_GOAL_KEY, 2000)
        UserProfile(
            displayName = name,
            weightKg = weight,
            heightCm = height,
            strideLengthMeters = stride,
            preferredUnits = units,
            dailyStepGoal = stepGoal,
            dailyWaterGoalMl = waterGoal
        )
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        // Write to EncryptedSharedPreferences and then bump a DataStore preference
        // so that `observeUserProfile()` (which maps `dataStore.data`) emits updated values.
        // Use withContext(Dispatchers.IO) to perform blocking file I/O off the main thread.
        withContext(Dispatchers.IO) {
            val editor = securePrefs.edit()
            val displayName = profile.displayName?.trim().orEmpty()
            if (displayName.isBlank()) {
                editor.remove(PROFILE_NAME_KEY)
            } else {
                editor.putString(PROFILE_NAME_KEY, displayName)
            }

            if (profile.weightKg != null) {
                editor.putString(PROFILE_WEIGHT_KEY, profile.weightKg.toString())
            } else {
                editor.remove(PROFILE_WEIGHT_KEY)
            }

            if (profile.heightCm != null) {
                editor.putInt(PROFILE_HEIGHT_KEY, profile.heightCm)
            } else {
                editor.remove(PROFILE_HEIGHT_KEY)
            }

            editor.putString(PROFILE_STRIDE_KEY, profile.strideLengthMeters.toString())
            editor.putString(PROFILE_UNITS_KEY, profile.preferredUnits)
            editor.putInt(PROFILE_STEP_GOAL_KEY, profile.dailyStepGoal)
            editor.putInt(PROFILE_WATER_GOAL_KEY, profile.dailyWaterGoalMl)
            // commit() is synchronous and returns boolean; prefer to ensure data persisted before emitting
            editor.commit()

            // Touch DataStore to force emission so observers read updated securePrefs values
            dataStore.edit { prefs ->
                val current = prefs[PROFILE_VERSION] ?: 0
                prefs[PROFILE_VERSION] = current + 1
            }
        }
    }

    // Convenience blocking read (use sparingly)
    fun getUserProfileBlocking(): UserProfile {
        // Direct synchronous read from secure prefs (convenience helper)
        val name = securePrefs.getString(PROFILE_NAME_KEY, null)
        val weight = securePrefs.getString(PROFILE_WEIGHT_KEY, null)?.toDoubleOrNull()
        val height = securePrefs.getInt(PROFILE_HEIGHT_KEY, -1).let { if (it <= 0) null else it }
        val stride = securePrefs.getString(PROFILE_STRIDE_KEY, null)?.toDoubleOrNull() ?: 0.78
        val units = securePrefs.getString(PROFILE_UNITS_KEY, "metric") ?: "metric"
        val stepGoal = securePrefs.getInt(PROFILE_STEP_GOAL_KEY, 10000)
        val waterGoal = securePrefs.getInt(PROFILE_WATER_GOAL_KEY, 2000)
        return UserProfile(
            displayName = name,
            weightKg = weight,
            heightCm = height,
            strideLengthMeters = stride,
            preferredUnits = units,
            dailyStepGoal = stepGoal,
            dailyWaterGoalMl = waterGoal
        )
    }
}

