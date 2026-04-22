package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedProtoUserProfileStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun writeThenRead_roundTripsAtomically() = runTest {
        val file = File(tempFolder.root, "secure_user_profile.pb")
        val store = EncryptedProtoUserProfileStore.create(file)

        val record = SecureStoredUserProfile(
            profile = UserProfile(
                displayName = "Taylor",
                weightKg = 72.4,
                heightCm = 177,
                strideLengthMeters = 0.84,
                preferredUnits = "metric",
                dailyStepGoal = 9000,
                dailyWaterGoalMl = 2300,
                nightlySleepGoalMinutes = 495,
                typicalBedtimeMinutes = 1370,
                typicalWakeTimeMinutes = 410,
                sleepDetectionBufferMinutes = 25,
            ),
            migrationState = ProfileMigrationState.MIGRATING,
            migratedAtEpochMs = 42L,
        )

        store.write(record)
        val restored = store.read()

        assertEquals(record.profile, restored.profile)
        assertEquals(ProfileMigrationState.MIGRATING, restored.migrationState)
        assertEquals(42L, restored.migratedAtEpochMs)
        assertEquals(SecureStoredUserProfile.CURRENT_SCHEMA_VERSION, restored.schemaVersion)
        assert(restored.lastWriteEpochMs > 0L)
    }

    @Test
    fun corruptedPayload_fallsBackToDefaults() = runTest {
        val file = File(tempFolder.root, "corrupted_user_profile.pb")
        file.writeText("not-a-proto-payload")
        val store = EncryptedProtoUserProfileStore.create(file)

        val restored = store.read()

        assertEquals(UserProfile(), restored.profile)
        assertEquals(ProfileMigrationState.NONE, restored.migrationState)
        assertEquals(SecureStoredUserProfile.CURRENT_SCHEMA_VERSION, restored.schemaVersion)
    }
}
