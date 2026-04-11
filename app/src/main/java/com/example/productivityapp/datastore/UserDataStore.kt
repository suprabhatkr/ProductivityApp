package com.example.productivityapp.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.app.data.model.WaterEntry
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.productivityapp.datastore.proto.WaterStoreProto
import com.example.productivityapp.datastore.proto.WaterDayProto
import com.example.productivityapp.datastore.proto.WaterEntryProto
import kotlinx.coroutines.Job

/**
 * Simple wrapper: encrypted SharedPreferences for sensitive user profile fields; Preferences DataStore
 * for per-day water counters and non-sensitive prefs.
 */
class UserDataStore(
    private val context: Context,
    /** Optional test-injection points so unit tests can provide their own DataStore / SharedPreferences. */
    private val injectedDataStore: DataStore<Preferences>? = null,
    private val injectedWaterProtoStore: DataStore<WaterStoreProto>? = null,
    private val injectedSecurePrefs: SharedPreferences? = null,
) {

    private val dataStore: DataStore<Preferences> = injectedDataStore ?: PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + Job()),
        produceFile = { context.preferencesDataStoreFile("user_prefs") }
    )

    private val securePrefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        injectedSecurePrefs ?: createSecurePrefs(context.applicationContext)
    }

    // Preferences keys (legacy water stored per-date as int / JSON entries)
    private fun waterKeyForDate(date: String) = intPreferencesKey("water_" + date)
    private fun entriesKeyForDate(date: String) = stringPreferencesKey("entries_" + date)
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
        val entriesKey = entriesKeyForDate(date)
        dataStore.edit { prefs ->
            prefs.remove(key)
            prefs.remove(entriesKey)
        }
    }
    /** Observe the list of water entries for a date. Proto-backed. */
    fun observeEntriesForDate(date: String): Flow<List<WaterEntry>> {
        // Trigger lazy migration if legacy JSON exists for this date
        performLegacyMigrationIfPresent(date)

        return (injectedWaterProtoStore ?: createWaterProtoStore()).data.map { store ->
            val day = store.daysMap[date]
            if (day == null) emptyList()
            else day.entriesList.map { proto ->
                // map proto -> domain
                val ldt = try {
                    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(proto.timestampEpochMs), ZoneId.systemDefault())
                } catch (_: Exception) {
                    LocalDateTime.now()
                }
                WaterEntry(id = proto.id, amountMl = proto.amountMl, timestamp = ldt)
            }
        }
    }

    private fun createWaterProtoStore(): DataStore<WaterStoreProto> =
        injectedWaterProtoStore ?: DataStoreFactory.create(
            serializer = WaterStoreSerializer,
            scope = CoroutineScope(Dispatchers.IO + Job()),
            produceFile = { context.preferencesDataStoreFile("water_store.pb") }
        )

    // Migrate legacy JSON entries (stored in Preferences) to the proto store for a single date.
    private fun performLegacyMigrationIfPresent(date: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val key = entriesKeyForDate(date)
                val prefsSnapshot = dataStore.data.first()
                val json = prefsSnapshot[key]
                if (!json.isNullOrBlank()) {
                    val arr = JSONArray(json)
                    val protoStore = createWaterProtoStore()
                    protoStore.updateData { current ->
                        val builder = current.toBuilder()
                        val existing = current.daysMap[date]
                        val dayBuilder = existing?.toBuilder() ?: WaterDayProto.newBuilder()
                        var total = dayBuilder.totalMl
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val id = obj.optLong("id", System.currentTimeMillis())
                            val amount = obj.optInt("amountMl", 0)
                            val ts = obj.optString("timestamp", null)
                            val epochMs = try {
                                if (ts != null) {
                                    java.time.LocalDateTime.parse(ts).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                } else System.currentTimeMillis()
                            } catch (_: Exception) {
                                System.currentTimeMillis()
                            }
                            val entryProto = WaterEntryProto.newBuilder()
                                .setId(id)
                                .setAmountMl(amount)
                                .setTimestampEpochMs(epochMs)
                                .build()
                            dayBuilder.addEntries(entryProto)
                            total += amount
                        }
                        dayBuilder.totalMl = total
                        builder.putDays(date, dayBuilder.build())
                        builder.build()
                    }

                    // remove legacy pref key after successful migration
                    dataStore.edit { prefs -> prefs.remove(entriesKeyForDate(date)) }
                }
            } catch (_: Exception) {
                // swallow migration errors to avoid data loss or crashes
            }
        }
    }

    /** Add an entry and return its generated id. Proto-backed. */
    suspend fun addEntryReturnId(date: String, amountMl: Int): Long {
        val id = System.currentTimeMillis()
        val nowEpoch = System.currentTimeMillis()

        // ensure any legacy entries migrated first for this date
        performLegacyMigrationIfPresent(date)

        val protoStore = createWaterProtoStore()
        protoStore.updateData { current ->
            val builder = current.toBuilder()
            val existing = current.daysMap[date]
            val dayBuilder = existing?.toBuilder() ?: WaterDayProto.newBuilder()
            val entryProto = WaterEntryProto.newBuilder()
                .setId(id)
                .setAmountMl(amountMl)
                .setTimestampEpochMs(nowEpoch)
                .build()
            dayBuilder.addEntries(entryProto)
            dayBuilder.totalMl = dayBuilder.totalMl + amountMl
            builder.putDays(date, dayBuilder.build())
            builder.build()
        }
        return id
    }

    suspend fun removeEntry(date: String, id: Long) {
        val protoStore = createWaterProtoStore()
        protoStore.updateData { current ->
            val builder = current.toBuilder()
            val day = current.daysMap[date] ?: return@updateData current
            val dayBuilder = day.toBuilder()
            var removedAmount = 0
            val newEntries = day.entriesList.filter { entry ->
                val keep = entry.id != id
                if (!keep) removedAmount = entry.amountMl
                keep
            }
            dayBuilder.clearEntries()
            dayBuilder.addAllEntries(newEntries)
            dayBuilder.totalMl = (day.totalMl - removedAmount).coerceAtLeast(0)
            builder.putDays(date, dayBuilder.build())
            builder.build()
        }
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

    /**
     * AndroidX Security Crypto's current SharedPreferences wrapper is deprecated but still the
     * compatibility-safe option for the existing on-device encrypted profile store in this app.
     * We isolate it here so the rest of the repository stays warning-free and the storage format
     * remains unchanged until a deliberate migration is planned.
     */
    @Suppress("DEPRECATION")
    private fun createSecurePrefs(appContext: Context): SharedPreferences {
        val masterKey = androidx.security.crypto.MasterKey.Builder(appContext)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()

        return androidx.security.crypto.EncryptedSharedPreferences.create(
            appContext,
            "user_profile_encrypted",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}

