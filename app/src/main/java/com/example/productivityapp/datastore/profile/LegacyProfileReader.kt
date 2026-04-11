package com.example.productivityapp.datastore.profile

import android.content.Context
import android.content.SharedPreferences

private const val LEGACY_PREFS_FILE = "user_profile_encrypted"

data class LegacyProfileSnapshot(
    val displayName: String?,
    val weightKgRaw: String?,
    val heightCm: Int?,
    val strideLengthMetersRaw: String?,
    val preferredUnits: String?,
    val dailyStepGoal: Int?,
    val dailyWaterGoalMl: Int?,
)

interface LegacyProfileReader {
    suspend fun readLegacyProfile(): LegacyProfileSnapshot?
}

class SharedPreferencesLegacyProfileReader(
    private val sharedPreferences: SharedPreferences,
) : LegacyProfileReader {
    override suspend fun readLegacyProfile(): LegacyProfileSnapshot? {
        return LegacyProfileSnapshot(
            displayName = sharedPreferences.getString("profile_name", null),
            weightKgRaw = sharedPreferences.getString("profile_weight", null),
            heightCm = sharedPreferences.getInt("profile_height", -1).takeIf { it > 0 },
            strideLengthMetersRaw = sharedPreferences.getString("profile_stride", null),
            preferredUnits = sharedPreferences.getString("profile_units", null),
            dailyStepGoal = sharedPreferences.getInt("profile_step_goal", -1).takeIf { it > 0 },
            dailyWaterGoalMl = sharedPreferences.getInt("profile_water_goal", -1).takeIf { it > 0 },
        ).takeIf {
            it.displayName != null ||
                it.weightKgRaw != null ||
                it.heightCm != null ||
                it.strideLengthMetersRaw != null ||
                it.preferredUnits != null ||
                it.dailyStepGoal != null ||
                it.dailyWaterGoalMl != null
        }
    }

    companion object {
        fun fromContext(context: Context): SharedPreferencesLegacyProfileReader {
            val prefs = context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
            return SharedPreferencesLegacyProfileReader(prefs)
        }
    }
}

