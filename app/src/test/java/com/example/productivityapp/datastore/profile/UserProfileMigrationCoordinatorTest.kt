package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserProfileMigrationCoordinatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun disabledMigration_returnsDisabledWithoutReadingLegacy() = runTest {
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = FakeSecureStore(),
            legacyProfileReader = FakeLegacyReader(
                LegacyProfileSnapshot("Chris", "80.0", 180, "0.9", "metric", 10000, 2000)
            ),
            migrationEnabled = false,
        )

        val result = coordinator.migrateIfNeeded()

        assertEquals(UserProfileMigrationResult.Disabled, result)
    }

    @Test
    fun alreadyComplete_returnsAlreadyComplete() = runTest {
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = FakeSecureStore(
                SecureStoredUserProfile(
                    profile = UserProfile(displayName = "Pat"),
                    migrationState = ProfileMigrationState.COMPLETE,
                )
            ),
            legacyProfileReader = FakeLegacyReader(null),
            migrationEnabled = true,
        )

        val result = coordinator.migrateIfNeeded()

        assertEquals(UserProfileMigrationResult.AlreadyComplete, result)
    }

    @Test
    fun enabledMigrationWithLegacyData_writesAndFinalizesMigration() = runTest {
        val legacy = LegacyProfileSnapshot("Sam", "bad", 172, "0.81", "metric", 12000, 2500)
        val secureStore = FakeSecureStore()
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = secureStore,
            legacyProfileReader = FakeLegacyReader(legacy),
            migrationEnabled = true,
            clock = { 123456L },
        )

        val result = coordinator.migrateIfNeeded()

        assertTrue(result is UserProfileMigrationResult.Migrated)
        val migrated = (result as UserProfileMigrationResult.Migrated).record
        assertEquals(ProfileMigrationState.COMPLETE, migrated.migrationState)
        assertEquals(123456L, migrated.migratedAtEpochMs)
        assertEquals("Sam", migrated.profile.displayName)
        assertEquals(null, migrated.profile.weightKg)
        assertEquals(172, migrated.profile.heightCm)
        assertEquals(0.81, migrated.profile.strideLengthMeters, 0.0)
        assertEquals(12000, migrated.profile.dailyStepGoal)
        assertEquals(2500, migrated.profile.dailyWaterGoalMl)
        assertTrue(secureStore.read().lastWriteEpochMs > 0L)
    }

    @Test
    fun enabledMigrationWithoutLegacyData_returnsNoLegacyData() = runTest {
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = FakeSecureStore(),
            legacyProfileReader = FakeLegacyReader(null),
            migrationEnabled = true,
        )

        val result = coordinator.migrateIfNeeded()

        assertEquals(UserProfileMigrationResult.NoLegacyData, result)
    }

    @Test
    fun verificationMismatch_returnsFailed() = runTest {
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = MismatchedReadSecureStore(),
            legacyProfileReader = FakeLegacyReader(
                LegacyProfileSnapshot("Sam", "70.0", 172, "0.81", "metric", 12000, 2500)
            ),
            migrationEnabled = true,
        )

        val result = coordinator.migrateIfNeeded()

        assertEquals(
            UserProfileMigrationResult.Failed(UserProfileMigrationFailureReason.VERIFY_FAILED),
            result,
        )
    }

    @Test
    fun migrationWithRealStore_persistsCompleteMarkerAndPayload() = runTest {
        val file = File(tempFolder.root, "migration-profile.pb")
        val secureStore = EncryptedProtoUserProfileStore.create(file)
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = secureStore,
            legacyProfileReader = FakeLegacyReader(
                LegacyProfileSnapshot("Riley", "68.2", 169, null, null, null, null)
            ),
            migrationEnabled = true,
            clock = { 999L },
        )

        val result = coordinator.migrateIfNeeded()

        assertTrue(result is UserProfileMigrationResult.Migrated)
        val persisted = secureStore.read()
        assertEquals(ProfileMigrationState.COMPLETE, persisted.migrationState)
        assertEquals(999L, persisted.migratedAtEpochMs)
        assertEquals("Riley", persisted.profile.displayName)
        assertEquals(68.2, persisted.profile.weightKg ?: 0.0, 0.0)
        assertEquals(169, persisted.profile.heightCm)
        assertEquals(0.78, persisted.profile.strideLengthMeters, 0.0)
        assertEquals("metric", persisted.profile.preferredUnits)
        assertEquals(10000, persisted.profile.dailyStepGoal)
        assertEquals(2000, persisted.profile.dailyWaterGoalMl)
    }

    @Test
    fun successfulMigration_secondRunIsIdempotent() = runTest {
        val secureStore = FakeSecureStore()
        val legacyReader = CountingLegacyReader(
            LegacyProfileSnapshot("Jamie", "61.5", 166, "0.79", "metric", 11000, 2100)
        )
        val coordinator = UserProfileMigrationCoordinator(
            secureStore = secureStore,
            legacyProfileReader = legacyReader,
            migrationEnabled = true,
            clock = { 77L },
        )

        val first = coordinator.migrateIfNeeded()
        val second = coordinator.migrateIfNeeded()

        assertTrue(first is UserProfileMigrationResult.Migrated)
        assertEquals(UserProfileMigrationResult.AlreadyComplete, second)
        assertEquals(1, legacyReader.readCount)
    }

    private class FakeSecureStore(
        initial: SecureStoredUserProfile = SecureStoredUserProfile(),
    ) : SecureUserProfileStore {
        private val state = MutableStateFlow(initial)

        override fun observe(): Flow<SecureStoredUserProfile> = state

        override suspend fun read(): SecureStoredUserProfile = state.value

        override suspend fun write(record: SecureStoredUserProfile) {
            state.value = record.copy(lastWriteEpochMs = record.lastWriteEpochMs.takeIf { it > 0L } ?: 1L)
        }

        override suspend fun clear() {
            state.value = SecureStoredUserProfile()
        }
    }

    private class FakeLegacyReader(
        private val snapshot: LegacyProfileSnapshot?,
    ) : LegacyProfileReader {
        override suspend fun readLegacyProfile(): LegacyProfileSnapshot? = snapshot
    }

    private class CountingLegacyReader(
        private val snapshot: LegacyProfileSnapshot?,
    ) : LegacyProfileReader {
        var readCount: Int = 0
            private set

        override suspend fun readLegacyProfile(): LegacyProfileSnapshot? {
            readCount += 1
            return snapshot
        }
    }

    private class MismatchedReadSecureStore : SecureUserProfileStore {
        private val state = MutableStateFlow(SecureStoredUserProfile())

        override fun observe(): Flow<SecureStoredUserProfile> = state

        override suspend fun read(): SecureStoredUserProfile = state.value.copy(
            profile = state.value.profile.copy(displayName = "tampered"),
            lastWriteEpochMs = 99L,
        )

        override suspend fun write(record: SecureStoredUserProfile) {
            state.value = record.copy(lastWriteEpochMs = 42L)
        }

        override suspend fun clear() {
            state.value = SecureStoredUserProfile()
        }
    }
}

