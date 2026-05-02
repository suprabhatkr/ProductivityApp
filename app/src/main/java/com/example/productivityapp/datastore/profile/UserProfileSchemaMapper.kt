package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.AvatarDefaults
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarPresentation
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.datastore.profile.proto.MigrationStateProto
import com.example.productivityapp.datastore.profile.proto.UserProfileProto

object UserProfileSchemaMapper {
    fun fromProto(proto: UserProfileProto): SecureStoredUserProfile {
        return SecureStoredUserProfile(
            profile = proto.toUserProfile(),
            schemaVersion = proto.schemaVersion.takeIf { it > 0 } ?: SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = proto.migrationState.toDomain(),
            migratedAtEpochMs = proto.migratedAtEpochMs,
            lastWriteEpochMs = proto.lastWriteEpochMs,
        )
    }

    fun toProto(record: SecureStoredUserProfile): UserProfileProto {
        val builder = UserProfileProto.newBuilder()
            .setSchemaVersion(record.schemaVersion)
            .setMigrationState(record.migrationState.toProto())
            .setMigratedAtEpochMs(record.migratedAtEpochMs)
            .setLastWriteEpochMs(record.lastWriteEpochMs)
        return builder.applyUserProfile(record.profile).build()
    }

    fun fromLegacy(snapshot: LegacyProfileSnapshot): SecureStoredUserProfile {
        // Legacy encrypted-pref key mapping from UserDataStore:
        // - profile_name       -> UserProfile.displayName
        // - profile_weight     -> UserProfile.weightKg (String -> Double?)
        // - profile_height     -> UserProfile.heightCm
        // - profile_stride     -> UserProfile.strideLengthMeters (String -> Double)
        // - profile_units      -> UserProfile.preferredUnits
        // - profile_step_goal  -> UserProfile.dailyStepGoal
        // - profile_water_goal -> UserProfile.dailyWaterGoalMl
        return SecureStoredUserProfile(
            profile = UserProfile(
                displayName = snapshot.displayName?.trim()?.takeIf { it.isNotBlank() },
                weightKg = snapshot.weightKgRaw?.toDoubleOrNull(),
                heightCm = snapshot.heightCm?.takeIf { it > 0 },
                strideLengthMeters = snapshot.strideLengthMetersRaw?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.78,
                preferredUnits = snapshot.preferredUnits?.takeIf { it.isNotBlank() } ?: "metric",
                dailyStepGoal = snapshot.dailyStepGoal?.takeIf { it > 0 } ?: 10000,
                dailyWaterGoalMl = snapshot.dailyWaterGoalMl?.takeIf { it > 0 } ?: 2000,
                nightlySleepGoalMinutes = 480,
                typicalBedtimeMinutes = 1320,
                typicalWakeTimeMinutes = 420,
                sleepDetectionBufferMinutes = 30,
                ageYears = snapshot.ageYears?.takeIf { it in 1..120 },
                gender = snapshot.gender?.takeIf { it.isNotBlank() },
            ),
            schemaVersion = SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = ProfileMigrationState.NONE,
        )
    }
}

private fun UserProfileProto.toUserProfile(): UserProfile {
    return UserProfile(
        displayName = displayName.takeIf { hasDisplayName() && it.isNotBlank() },
        weightKg = weightKg.takeIf { hasWeightKg() },
        heightCm = heightCm.takeIf { hasHeightCm() && it > 0 },
        strideLengthMeters = strideLengthMeters.takeIf { it > 0.0 } ?: 0.78,
        preferredUnits = preferredUnits.takeIf { it.isNotBlank() } ?: "metric",
        dailyStepGoal = dailyStepGoal.takeIf { it > 0 } ?: 10000,
        dailyWaterGoalMl = dailyWaterGoalMl.takeIf { it > 0 } ?: 2000,
        nightlySleepGoalMinutes = nightlySleepGoalMinutes.takeIf { it > 0 } ?: 480,
        typicalBedtimeMinutes = typicalBedtimeMinutes.takeIf { it in 0..1439 } ?: 1320,
        typicalWakeTimeMinutes = typicalWakeTimeMinutes.takeIf { it in 0..1439 } ?: 420,
        sleepDetectionBufferMinutes = sleepDetectionBufferMinutes.takeIf { it >= 0 } ?: 30,
        ageYears = ageYears.takeIf { hasAgeYears() && it in 1..120 },
        gender = gender.takeIf { hasGender() && it.isNotBlank() },
        avatar = AvatarConfig(
            avatarId = when {
                hasAvatarId() -> AvatarDefaults.normalizeAvatarId(avatarId)
                else -> AvatarDefaults.fallbackIdForLegacyPresentation(
                    AvatarPresentation.fromStorageValue(avatarPresentation.takeIf { hasAvatarPresentation() })
                )
            },
        ),
    )
}

private fun UserProfileProto.Builder.applyUserProfile(profile: UserProfile): UserProfileProto.Builder {
    setStrideLengthMeters(profile.strideLengthMeters)
    setPreferredUnits(profile.preferredUnits)
    setDailyStepGoal(profile.dailyStepGoal)
    setDailyWaterGoalMl(profile.dailyWaterGoalMl)
    setNightlySleepGoalMinutes(profile.nightlySleepGoalMinutes)
    setTypicalBedtimeMinutes(profile.typicalBedtimeMinutes)
    setTypicalWakeTimeMinutes(profile.typicalWakeTimeMinutes)
    setSleepDetectionBufferMinutes(profile.sleepDetectionBufferMinutes)
    setAvatarId(AvatarDefaults.normalizeAvatarId(profile.avatar.avatarId))
    clearAvatarSkinTone()
    clearAvatarPresentation()
    clearAvatarHairStyle()
    clearAvatarGlassesStyle()
    clearAvatarHatStyle()

    profile.displayName?.takeIf { it.isNotBlank() }?.let(::setDisplayName) ?: clearDisplayName()
    profile.weightKg?.let(::setWeightKg) ?: clearWeightKg()
    profile.heightCm?.let(::setHeightCm) ?: clearHeightCm()
    profile.ageYears?.let(::setAgeYears) ?: clearAgeYears()
    profile.gender?.takeIf { it.isNotBlank() }?.let(::setGender) ?: clearGender()
    return this
}

private fun MigrationStateProto.toDomain(): ProfileMigrationState = when (this) {
    MigrationStateProto.MIGRATION_STATE_MIGRATING -> ProfileMigrationState.MIGRATING
    MigrationStateProto.MIGRATION_STATE_COMPLETE -> ProfileMigrationState.COMPLETE
    MigrationStateProto.MIGRATION_STATE_FAILED -> ProfileMigrationState.FAILED
    MigrationStateProto.MIGRATION_STATE_NONE,
    MigrationStateProto.UNRECOGNIZED,
    -> ProfileMigrationState.NONE
}

private fun ProfileMigrationState.toProto(): MigrationStateProto = when (this) {
    ProfileMigrationState.NONE -> MigrationStateProto.MIGRATION_STATE_NONE
    ProfileMigrationState.MIGRATING -> MigrationStateProto.MIGRATION_STATE_MIGRATING
    ProfileMigrationState.COMPLETE -> MigrationStateProto.MIGRATION_STATE_COMPLETE
    ProfileMigrationState.FAILED -> MigrationStateProto.MIGRATION_STATE_FAILED
}
