package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.datastore.profile.proto.MigrationStateProto
import com.example.productivityapp.datastore.profile.proto.UserProfileProto

object UserProfileSchemaMapper {
    fun fromProto(proto: UserProfileProto): SecureStoredUserProfile {
        val profile = UserProfile(
            displayName = if (proto.hasDisplayName()) proto.displayName else null,
            weightKg = if (proto.hasWeightKg()) proto.weightKg else null,
            heightCm = if (proto.hasHeightCm()) proto.heightCm else null,
            strideLengthMeters = proto.strideLengthMeters.takeIf { it > 0.0 } ?: 0.78,
            preferredUnits = proto.preferredUnits.takeIf { it.isNotBlank() } ?: "metric",
            dailyStepGoal = proto.dailyStepGoal.takeIf { it > 0 } ?: 10000,
            dailyWaterGoalMl = proto.dailyWaterGoalMl.takeIf { it > 0 } ?: 2000,
        )
        return SecureStoredUserProfile(
            profile = profile,
            schemaVersion = proto.schemaVersion.takeIf { it > 0 } ?: SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = proto.migrationState.toDomain(),
            migratedAtEpochMs = proto.migratedAtEpochMs,
            lastWriteEpochMs = proto.lastWriteEpochMs,
        )
    }

    fun toProto(record: SecureStoredUserProfile): UserProfileProto {
        val builder = UserProfileProto.newBuilder()
            .setStrideLengthMeters(record.profile.strideLengthMeters)
            .setPreferredUnits(record.profile.preferredUnits)
            .setDailyStepGoal(record.profile.dailyStepGoal)
            .setDailyWaterGoalMl(record.profile.dailyWaterGoalMl)
            .setSchemaVersion(record.schemaVersion)
            .setMigrationState(record.migrationState.toProto())
            .setMigratedAtEpochMs(record.migratedAtEpochMs)
            .setLastWriteEpochMs(record.lastWriteEpochMs)

        record.profile.displayName?.takeIf { it.isNotBlank() }?.let(builder::setDisplayName)
        record.profile.weightKg?.let(builder::setWeightKg)
        record.profile.heightCm?.let(builder::setHeightCm)
        return builder.build()
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
            ),
            schemaVersion = SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = ProfileMigrationState.NONE,
        )
    }
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

