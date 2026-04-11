package com.example.productivityapp.datastore.profile

sealed interface UserProfileMigrationResult {
    data object Disabled : UserProfileMigrationResult
    data object AlreadyComplete : UserProfileMigrationResult
    data object NoLegacyData : UserProfileMigrationResult
    data class Pending(val legacySnapshot: LegacyProfileSnapshot?) : UserProfileMigrationResult
    data class Migrated(val record: SecureStoredUserProfile) : UserProfileMigrationResult
    data class Failed(val reason: UserProfileMigrationFailureReason) : UserProfileMigrationResult
}

enum class UserProfileMigrationFailureReason {
    WRITE_FAILED,
    VERIFY_FAILED,
    FINALIZE_FAILED,
}

/**
 * Slice-1 migration coordinator skeleton.
 *
 * This coordinator intentionally does not perform the full migration yet; it only exposes the
 * decision points and idempotent state checks needed by the next slice.
 */
class UserProfileMigrationCoordinator(
    private val secureStore: SecureUserProfileStore,
    private val legacyProfileReader: LegacyProfileReader,
    private val migrationEnabled: Boolean = false,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun migrateIfNeeded(): UserProfileMigrationResult {
        if (!migrationEnabled) return UserProfileMigrationResult.Disabled

        val existing = secureStore.read()
        if (existing.migrationState == ProfileMigrationState.COMPLETE) {
            return UserProfileMigrationResult.AlreadyComplete
        }

        val legacy = legacyProfileReader.readLegacyProfile() ?: return UserProfileMigrationResult.NoLegacyData
        val mapped = UserProfileSchemaMapper.fromLegacy(legacy)
        val migratingRecord = mapped.copy(
            schemaVersion = SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = ProfileMigrationState.MIGRATING,
            migratedAtEpochMs = 0L,
        )

        try {
            secureStore.write(migratingRecord)
        } catch (_: Throwable) {
            return UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.WRITE_FAILED)
        }

        val staged = try {
            secureStore.read()
        } catch (_: Throwable) {
            return UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.VERIFY_FAILED)
        }

        if (!staged.matchesMigrationExpectation(migratingRecord)) {
            return UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.VERIFY_FAILED)
        }

        val completedRecord = staged.copy(
            schemaVersion = SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = ProfileMigrationState.COMPLETE,
            migratedAtEpochMs = clock(),
        )

        try {
            secureStore.write(completedRecord)
        } catch (_: Throwable) {
            return UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.FINALIZE_FAILED)
        }

        val finalized = try {
            secureStore.read()
        } catch (_: Throwable) {
            return UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.FINALIZE_FAILED)
        }

        return if (finalized.matchesMigrationExpectation(completedRecord)) {
            UserProfileMigrationResult.Migrated(finalized)
        } else {
            UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.FINALIZE_FAILED)
        }
    }
}

private fun SecureStoredUserProfile.matchesMigrationExpectation(expected: SecureStoredUserProfile): Boolean {
    return profile == expected.profile &&
        schemaVersion == expected.schemaVersion &&
        migrationState == expected.migrationState &&
        migratedAtEpochMs == expected.migratedAtEpochMs &&
        lastWriteEpochMs > 0L
}

