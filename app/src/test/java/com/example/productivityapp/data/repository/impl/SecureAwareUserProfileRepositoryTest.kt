package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.datastore.profile.EncryptedProtoUserProfileStore
import com.example.productivityapp.datastore.profile.LegacyProfileReader
import com.example.productivityapp.datastore.profile.LegacyProfileSnapshot
import com.example.productivityapp.datastore.profile.ProfileMigrationState
import com.example.productivityapp.datastore.profile.SecureStoredUserProfile
import com.example.productivityapp.datastore.profile.SecureUserProfileStore
import com.example.productivityapp.datastore.profile.UserProfileMigrationCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SecureAwareUserProfileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun observeUserProfile_cutoverDisabled_usesLegacyFlow() = runTest {
        val legacyRepo = FakeLegacyRepository(UserProfile(displayName = "legacy", dailyWaterGoalMl = 2100))
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = legacyRepo,
            secureStore = FakeSecureStore(
                SecureStoredUserProfile(
                    profile = UserProfile(displayName = "secure"),
                    migrationState = ProfileMigrationState.COMPLETE,
                )
            ),
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = FakeSecureStore(),
                legacyProfileReader = FakeLegacyReader(null),
                migrationEnabled = false,
            ),
            enableSecureStoreCutover = false,
        )

        val observed = repo.observeUserProfile().first()

        assertEquals("legacy", observed.displayName)
        assertEquals(2100, observed.dailyWaterGoalMl)
    }

    @Test
    fun observeUserProfile_migrationEnabledCutoverDisabled_keepsLegacyFlowAndPopulatesSecureStore() = runTest {
        val secureStore = FakeSecureStore()
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(UserProfile(displayName = "legacy-flow", dailyWaterGoalMl = 2100)),
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("secure-copy", "65.0", 171, "0.81", "metric", 11111, 2222)
                ),
                migrationEnabled = true,
                clock = { 88L },
            ),
            enableSecureStoreCutover = false,
        )

        val observed = repo.observeUserProfile().first()
        val migrated = secureStore.read()

        assertEquals("legacy-flow", observed.displayName)
        assertEquals(2100, observed.dailyWaterGoalMl)
        assertEquals(ProfileMigrationState.COMPLETE, migrated.migrationState)
        assertEquals(88L, migrated.migratedAtEpochMs)
        assertEquals("secure-copy", migrated.profile.displayName)
        assertEquals(2222, migrated.profile.dailyWaterGoalMl)
    }

    @Test
    fun getUserProfileBlocking_migrationEnabledCutoverDisabled_keepsLegacyReadAndPopulatesSecureStore() {
        val secureStore = FakeSecureStore()
        val legacyProfile = UserProfile(displayName = "legacy-blocking", dailyWaterGoalMl = 2300)
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(legacyProfile),
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("secure-copy", "72.0", 180, "0.84", "metric", 12000, 2500)
                ),
                migrationEnabled = true,
                clock = { 99L },
            ),
            enableSecureStoreCutover = false,
        )

        val observed = repo.getUserProfileBlocking()
        val migrated = runBlockingRead(secureStore)

        assertEquals(legacyProfile, observed)
        assertEquals(ProfileMigrationState.COMPLETE, migrated.migrationState)
        assertEquals(99L, migrated.migratedAtEpochMs)
        assertEquals("secure-copy", migrated.profile.displayName)
    }

    @Test
    fun observeUserProfile_cutoverEnabled_prefersSecureStoreAfterMigration() = runTest {
        val secureStore = FakeSecureStore()
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(UserProfile(displayName = "legacy")),
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("secure", "67.5", 170, "0.8", "metric", 12345, 2400)
                ),
                migrationEnabled = true,
                clock = { 55L },
            ),
            enableSecureStoreCutover = true,
        )

        val observed = repo.observeUserProfile().first()

        assertEquals("secure", observed.displayName)
        assertEquals(67.5, observed.weightKg ?: 0.0, 0.0)
        assertEquals(170, observed.heightCm)
        assertEquals(12345, observed.dailyStepGoal)
        assertEquals(2400, observed.dailyWaterGoalMl)
    }

    @Test
    fun getUserProfileBlocking_cutoverEnabled_prefersSecureStoreAfterMigration() {
        val secureStore = FakeSecureStore()
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(UserProfile(displayName = "legacy", dailyWaterGoalMl = 2100)),
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("secure-blocking", "68.0", 172, "0.81", "metric", 13000, 2600)
                ),
                migrationEnabled = true,
                clock = { 101L },
            ),
            enableSecureStoreCutover = true,
        )

        val observed = repo.getUserProfileBlocking()

        assertEquals("secure-blocking", observed.displayName)
        assertEquals(68.0, observed.weightKg ?: 0.0, 0.0)
        assertEquals(172, observed.heightCm)
        assertEquals(13000, observed.dailyStepGoal)
        assertEquals(2600, observed.dailyWaterGoalMl)
    }

    @Test
    fun observeUserProfile_cutoverEnabled_incompleteSecureStateFallsBackToLegacy() = runTest {
        val legacyProfile = UserProfile(displayName = "legacy-fallback", dailyWaterGoalMl = 2400)
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(legacyProfile),
            secureStore = FakeSecureStore(
                SecureStoredUserProfile(
                    profile = UserProfile(displayName = "partial"),
                    migrationState = ProfileMigrationState.MIGRATING,
                )
            ),
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = FakeSecureStore(
                    SecureStoredUserProfile(
                        profile = UserProfile(displayName = "partial"),
                        migrationState = ProfileMigrationState.MIGRATING,
                    )
                ),
                legacyProfileReader = FakeLegacyReader(null),
                migrationEnabled = false,
            ),
            enableSecureStoreCutover = true,
        )

        val observed = repo.observeUserProfile().first()

        assertEquals(legacyProfile, observed)
    }

    @Test
    fun observeUserProfile_cutoverEnabled_secureObserveFailureFallsBackToLegacy() = runTest {
        val legacyProfile = UserProfile(displayName = "legacy-flow-fallback", dailyWaterGoalMl = 2450)
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(legacyProfile),
            secureStore = ThrowingObserveSecureStore(),
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = CompletedButThrowingObserveSecureStore(),
                legacyProfileReader = FakeLegacyReader(null),
                migrationEnabled = true,
            ),
            enableSecureStoreCutover = true,
        )

        val observed = repo.observeUserProfile().first()

        assertEquals(legacyProfile, observed)
    }

    @Test
    fun getUserProfileBlocking_secureFailureFallsBackToLegacy() {
        val legacyProfile = UserProfile(displayName = "legacy", dailyWaterGoalMl = 2200)
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(legacyProfile),
            secureStore = ThrowingSecureStore(),
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = ThrowingSecureStore(),
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("secure", "66.0", 168, "0.79", "metric", 10000, 2000)
                ),
                migrationEnabled = true,
            ),
            enableSecureStoreCutover = true,
        )

        val result = repo.getUserProfileBlocking()

        assertEquals(legacyProfile, result)
    }

    @Test
    fun getUserProfileBlocking_cutoverEnabled_incompleteStateFallsBackToLegacy() {
        val legacyProfile = UserProfile(displayName = "legacy-incomplete", dailyWaterGoalMl = 2150)
        val incompleteStore = FakeSecureStore(
            SecureStoredUserProfile(
                profile = UserProfile(displayName = "partial"),
                migrationState = ProfileMigrationState.MIGRATING,
            )
        )
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(legacyProfile),
            secureStore = incompleteStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = incompleteStore,
                legacyProfileReader = FakeLegacyReader(null),
                migrationEnabled = false,
            ),
            enableSecureStoreCutover = true,
        )

        val observed = repo.getUserProfileBlocking()

        assertEquals(legacyProfile, observed)
    }

    @Test
    fun updateUserProfile_afterCompleteMigration_dualWritesToSecureStore() = runTest {
        val legacyRepo = FakeLegacyRepository(UserProfile(displayName = "before"))
        val secureStore = FakeSecureStore(
            SecureStoredUserProfile(
                profile = UserProfile(displayName = "before"),
                migrationState = ProfileMigrationState.COMPLETE,
                migratedAtEpochMs = 77L,
                lastWriteEpochMs = 1L,
            )
        )
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = legacyRepo,
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(null),
                migrationEnabled = true,
            ),
            enableSecureStoreCutover = true,
        )

        val updated = UserProfile(
            displayName = "after",
            weightKg = 71.2,
            heightCm = 176,
            strideLengthMeters = 0.83,
            preferredUnits = "imperial",
            dailyStepGoal = 11111,
            dailyWaterGoalMl = 2550,
        )
        repo.updateUserProfile(updated)

        assertEquals(updated, legacyRepo.profile.value)
        assertEquals(updated, secureStore.read().profile)
        assertEquals(ProfileMigrationState.COMPLETE, secureStore.read().migrationState)
        assertEquals(77L, secureStore.read().migratedAtEpochMs)
    }

    @Test
    fun repositoryDrivenMigration_withRealStore_populatesSecureFileWhileCutoverStaysOff() = runTest {
        val file = File(tempFolder.root, "runtime-secure-profile.pb")
        val secureStore = EncryptedProtoUserProfileStore.create(file)
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(UserProfile(displayName = "legacy-runtime", dailyWaterGoalMl = 2000)),
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("runtime-copy", "69.4", 175, "0.82", "metric", 12300, 2400)
                ),
                migrationEnabled = true,
                clock = { 1234L },
            ),
            enableSecureStoreCutover = false,
        )

        val observed = repo.observeUserProfile().first()
        val migrated = secureStore.read()

        assertEquals("legacy-runtime", observed.displayName)
        assertTrue(file.exists())
        assertEquals(ProfileMigrationState.COMPLETE, migrated.migrationState)
        assertEquals(1234L, migrated.migratedAtEpochMs)
        assertEquals("runtime-copy", migrated.profile.displayName)
        assertEquals(69.4, migrated.profile.weightKg ?: 0.0, 0.0)
        assertEquals(2400, migrated.profile.dailyWaterGoalMl)
    }

    @Test
    fun migration_doesNotRemoveLegacyData_afterPopulation() = runTest {
        val legacyRepo = FakeLegacyRepository(UserProfile(displayName = "orig", dailyWaterGoalMl = 2020))
        val secureStore = FakeSecureStore()

        val repo = SecureAwareUserProfileRepository(
            legacyRepository = legacyRepo,
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("secure-copy", "70.0", 170, "0.8", "metric", 10000, 2100)
                ),
                migrationEnabled = true,
                clock = { 222L },
            ),
            enableSecureStoreCutover = false,
        )

        // Trigger repository-driven migration via observe
        val observed = repo.observeUserProfile().first()

        // App should still read legacy profile and legacy storage should remain unchanged
        assertEquals("orig", observed.displayName)
        assertEquals(UserProfile(displayName = "orig", dailyWaterGoalMl = 2020), legacyRepo.profile.value)
        // secure store should have been populated
        val migrated = secureStore.read()
        assertEquals(ProfileMigrationState.COMPLETE, migrated.migrationState)
        assertEquals(222L, migrated.migratedAtEpochMs)
    }

    @Test
    fun repositoryDrivenReadCutover_withRealStore_returnsSecureProfileAndKeepsFileReadable() = runTest {
        val file = File(tempFolder.root, "runtime-secure-profile-cutover.pb")
        val secureStore = EncryptedProtoUserProfileStore.create(file)
        val repo = SecureAwareUserProfileRepository(
            legacyRepository = FakeLegacyRepository(UserProfile(displayName = "legacy-runtime", dailyWaterGoalMl = 2000)),
            secureStore = secureStore,
            migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = FakeLegacyReader(
                    LegacyProfileSnapshot("runtime-secure", "70.4", 176, "0.83", "metric", 12600, 2450)
                ),
                migrationEnabled = true,
                clock = { 5678L },
            ),
            enableSecureStoreCutover = true,
        )

        val observed = repo.getUserProfileBlocking()
        val persisted = secureStore.read()

        assertTrue(file.exists())
        assertEquals("runtime-secure", observed.displayName)
        assertEquals(70.4, observed.weightKg ?: 0.0, 0.0)
        assertEquals(ProfileMigrationState.COMPLETE, persisted.migrationState)
        assertEquals(5678L, persisted.migratedAtEpochMs)
        assertEquals("runtime-secure", persisted.profile.displayName)
    }

    private class FakeLegacyRepository(initial: UserProfile) : UserProfileRepository {
        val profile = MutableStateFlow(initial)

        override fun observeUserProfile(): Flow<UserProfile> = profile

        override fun getUserProfileBlocking(): UserProfile = profile.value

        override suspend fun updateUserProfile(profile: UserProfile) {
            this.profile.value = profile
        }
    }

    private class FakeSecureStore(
        initial: SecureStoredUserProfile = SecureStoredUserProfile(),
    ) : SecureUserProfileStore {
        private val state = MutableStateFlow(initial)

        override fun observe(): Flow<SecureStoredUserProfile> = state

        override suspend fun read(): SecureStoredUserProfile = state.value

        override suspend fun write(record: SecureStoredUserProfile) {
            state.value = record.copy(
                lastWriteEpochMs = record.lastWriteEpochMs.takeIf { it > 0L } ?: 1L,
            )
        }

        override suspend fun clear() {
            state.value = SecureStoredUserProfile()
        }
    }

    private class ThrowingSecureStore : SecureUserProfileStore {
        override fun observe(): Flow<SecureStoredUserProfile> = MutableStateFlow(SecureStoredUserProfile())

        override suspend fun read(): SecureStoredUserProfile {
            throw IllegalStateException("secure store unavailable")
        }

        override suspend fun write(record: SecureStoredUserProfile) {
            throw IllegalStateException("secure store unavailable")
        }

        override suspend fun clear() {
            throw IllegalStateException("secure store unavailable")
        }
    }

    private class ThrowingObserveSecureStore : SecureUserProfileStore {
        override fun observe(): Flow<SecureStoredUserProfile> = flow {
            throw IllegalStateException("secure observe unavailable")
        }

        override suspend fun read(): SecureStoredUserProfile = SecureStoredUserProfile(
            profile = UserProfile(displayName = "unused"),
            migrationState = ProfileMigrationState.COMPLETE,
            lastWriteEpochMs = 1L,
        )

        override suspend fun write(record: SecureStoredUserProfile) = Unit

        override suspend fun clear() = Unit
    }

    private class CompletedButThrowingObserveSecureStore : SecureUserProfileStore {
        override fun observe(): Flow<SecureStoredUserProfile> = flow {
            throw IllegalStateException("secure observe unavailable")
        }

        override suspend fun read(): SecureStoredUserProfile = SecureStoredUserProfile(
            profile = UserProfile(displayName = "completed"),
            migrationState = ProfileMigrationState.COMPLETE,
            migratedAtEpochMs = 1L,
            lastWriteEpochMs = 1L,
        )

        override suspend fun write(record: SecureStoredUserProfile) = Unit

        override suspend fun clear() = Unit
    }

    private class FakeLegacyReader(
        private val snapshot: LegacyProfileSnapshot?,
    ) : LegacyProfileReader {
        override suspend fun readLegacyProfile(): LegacyProfileSnapshot? = snapshot
    }

    private fun runBlockingRead(store: SecureUserProfileStore): SecureStoredUserProfile =
        kotlinx.coroutines.runBlocking { store.read() }
}


