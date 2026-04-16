package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

enum class ProfileMigrationState {
    NONE,
    MIGRATING,
    COMPLETE,
    FAILED,
}

data class SecureStoredUserProfile(
    val profile: UserProfile = UserProfile(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val migrationState: ProfileMigrationState = ProfileMigrationState.NONE,
    val migratedAtEpochMs: Long = 0L,
    val lastWriteEpochMs: Long = 0L,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

interface SecureUserProfileStore {
    fun observe(): Flow<SecureStoredUserProfile>
    suspend fun read(): SecureStoredUserProfile
    suspend fun write(record: SecureStoredUserProfile)
    suspend fun clear()
}
