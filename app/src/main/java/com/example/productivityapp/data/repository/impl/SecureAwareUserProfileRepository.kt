package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.datastore.profile.ProfileMigrationState
import com.example.productivityapp.datastore.profile.SecureStoredUserProfile
import com.example.productivityapp.datastore.profile.SecureUserProfileStore
import com.example.productivityapp.datastore.profile.UserProfileMigrationCoordinator
import com.example.productivityapp.datastore.profile.UserProfileMigrationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Hybrid repository used for the secure-profile migration rollout.
 *
 * Behavior:
 * - current legacy repository remains the source of truth until secure cutover is enabled and the
 *   secure store is known-complete
 * - secure-store reads are preferred only after migration completion
 * - legacy writes are always preserved for rollback safety
 * - secure writes are best-effort mirrors after cutover is active
 */
class SecureAwareUserProfileRepository(
    private val legacyRepository: UserProfileRepository,
    private val secureStore: SecureUserProfileStore,
    private val migrationCoordinator: UserProfileMigrationCoordinator,
    private val enableSecureStoreCutover: Boolean,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UserProfileRepository {

    override fun observeUserProfile(): Flow<UserProfile> {
        return flow {
            val migrationResult = triggerMigrationBestEffort()
            if (enableSecureStoreCutover && shouldUseSecureStore(migrationResult)) {
                try {
                    emitAll(secureStore.observe().map { it.profile })
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    emitAll(legacyRepository.observeUserProfile())
                }
            } else {
                emitAll(legacyRepository.observeUserProfile())
            }
        }
    }

    override fun getUserProfileBlocking(): UserProfile {
        return runCatching {
            runBlocking(ioDispatcher) {
                val migrationResult = triggerMigrationBestEffort()
                if (!enableSecureStoreCutover) {
                    legacyRepository.getUserProfileBlocking()
                } else {
                    when (migrationResult) {
                        is UserProfileMigrationResult.Migrated -> migrationResult.record.profile
                        UserProfileMigrationResult.AlreadyComplete -> secureStore.read().profile
                        else -> legacyRepository.getUserProfileBlocking()
                    }
                }
            }
        }.getOrElse {
            legacyRepository.getUserProfileBlocking()
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile) {
        legacyRepository.updateUserProfile(profile)
        when (val result = triggerMigrationBestEffort()) {
            UserProfileMigrationResult.AlreadyComplete -> mirrorToSecureStore(profile)
            is UserProfileMigrationResult.Migrated -> {
                if (result.record.profile != profile) {
                    mirrorToSecureStore(profile)
                }
            }
            else -> Unit
        }
    }

    private suspend fun shouldUseSecureStore(
        migrationResult: UserProfileMigrationResult? = null,
    ): Boolean {
        return when (migrationResult ?: triggerMigrationBestEffort()) {
            is UserProfileMigrationResult.Migrated,
            UserProfileMigrationResult.AlreadyComplete,
            -> runCatching {
                secureStore.read().migrationState == ProfileMigrationState.COMPLETE
            }.getOrDefault(false)

            else -> false
        }
    }

    private suspend fun triggerMigrationBestEffort(): UserProfileMigrationResult? {
        return runCatching { migrationCoordinator.migrateIfNeeded() }.getOrNull()
    }

    private suspend fun mirrorToSecureStore(profile: UserProfile) {
        runCatching {
            val current = secureStore.read()
            if (current.migrationState != ProfileMigrationState.COMPLETE) return
            secureStore.write(
                current.copy(
                    profile = profile,
                    schemaVersion = current.schemaVersion.takeIf { it > 0 }
                        ?: SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
                    migrationState = ProfileMigrationState.COMPLETE,
                    migratedAtEpochMs = current.migratedAtEpochMs.takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                )
            )
        }
    }
}


