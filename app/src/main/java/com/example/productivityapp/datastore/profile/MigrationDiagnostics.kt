package com.example.productivityapp.datastore.profile

data class MigrationStatus(
    val schemaVersion: Int,
    val migrationState: ProfileMigrationState,
    val migratedAtEpochMs: Long,
    val lastWriteEpochMs: Long,
    val legacyPresent: Boolean,
    val legacySnapshot: LegacyProfileSnapshot?,
    val lastMigrationResult: UserProfileMigrationResult? = null,
)

class MigrationDiagnostics(
    private val secureStore: SecureUserProfileStore,
    private val legacyReader: LegacyProfileReader,
    private val coordinator: UserProfileMigrationCoordinator,
) {
    suspend fun readStatus(): MigrationStatus {
        val secure = runCatching { secureStore.read() }.getOrNull()
        val legacy = runCatching { legacyReader.readLegacyProfile() }.getOrNull()

        return MigrationStatus(
            schemaVersion = secure?.schemaVersion ?: -1,
            migrationState = secure?.migrationState ?: ProfileMigrationState.NONE,
            migratedAtEpochMs = secure?.migratedAtEpochMs ?: -1L,
            lastWriteEpochMs = secure?.lastWriteEpochMs ?: -1L,
            legacyPresent = legacy != null,
            legacySnapshot = legacy,
            lastMigrationResult = null,
        )
    }

    suspend fun triggerMigration(): UserProfileMigrationResult {
        return coordinator.migrateIfNeeded()
    }

    suspend fun clearSecureStore() {
        secureStore.clear()
    }
}

