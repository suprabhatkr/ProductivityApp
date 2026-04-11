package com.example.productivityapp.data

import android.content.Context
import com.example.productivityapp.data.repository.RunRepository
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.repository.impl.DataStoreUserProfileRepository
import com.example.productivityapp.data.repository.impl.RoomRunRepository
import com.example.productivityapp.data.repository.impl.RoomSleepRepository
import com.example.productivityapp.data.repository.impl.RoomStepRepository
import com.example.productivityapp.data.repository.impl.SecureAwareUserProfileRepository
import com.example.productivityapp.datastore.UserDataStore
import com.example.productivityapp.datastore.profile.EncryptedProtoUserProfileStore
import com.example.productivityapp.datastore.profile.SharedPreferencesLegacyProfileReader
import com.example.productivityapp.datastore.profile.UserProfileMigrationCoordinator

object RepositoryProvider {
    private const val ENABLE_SECURE_PROFILE_MIGRATION = true
    private const val ENABLE_SECURE_PROFILE_CUTOVER = true

    @Volatile
    private var userProfileRepository: UserProfileRepository? = null

    fun provideStepRepository(context: Context): StepRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomStepRepository(db.stepDao())
    }

    fun provideRunRepository(context: Context): RunRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomRunRepository(db)
    }

    fun provideSleepRepository(context: Context): SleepRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomSleepRepository(db.sleepDao())
    }

    fun provideUserProfileRepository(context: Context): UserProfileRepository {
        userProfileRepository?.let { return it }

        return synchronized(this) {
            userProfileRepository?.let { return@synchronized it }

            val appContext = context.applicationContext
            val legacyStore = UserDataStore(appContext)
            val legacyRepository = DataStoreUserProfileRepository(legacyStore)
            val secureStore = EncryptedProtoUserProfileStore.create(
                appContext.filesDir.resolve("secure_user_profile.pb")
            )
            val migrationCoordinator = UserProfileMigrationCoordinator(
                secureStore = secureStore,
                legacyProfileReader = SharedPreferencesLegacyProfileReader.fromContext(appContext),
                migrationEnabled = ENABLE_SECURE_PROFILE_MIGRATION,
            )

            SecureAwareUserProfileRepository(
                legacyRepository = legacyRepository,
                secureStore = secureStore,
                migrationCoordinator = migrationCoordinator,
                enableSecureStoreCutover = ENABLE_SECURE_PROFILE_CUTOVER,
            ).also { userProfileRepository = it }
        }
    }
}

