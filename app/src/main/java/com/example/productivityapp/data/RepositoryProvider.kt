package com.example.productivityapp.data

import android.content.Context
import com.example.productivityapp.data.repository.AppThemeRepository
import com.example.productivityapp.data.repository.MindfulnessRepository
import com.example.productivityapp.data.repository.RunRepository
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.repository.WaterRepository
import com.example.productivityapp.data.repository.WorkoutRepository
import com.example.productivityapp.data.repository.impl.DataStoreAppThemeRepository
import com.example.productivityapp.data.repository.impl.DataStoreUserProfileRepository
import com.example.productivityapp.data.repository.impl.DataStoreWaterRepository
import com.example.productivityapp.data.repository.impl.RoomMindfulnessRepository
import com.example.productivityapp.data.repository.impl.RoomRunRepository
import com.example.productivityapp.data.repository.impl.RoomSleepRepository
import com.example.productivityapp.data.repository.impl.RoomStepRepository
import com.example.productivityapp.data.repository.impl.RoomWorkoutRepository
import com.example.productivityapp.data.repository.impl.SecureAwareUserProfileRepository
import com.example.productivityapp.datastore.UserDataStore
import com.example.productivityapp.datastore.profile.EncryptedProtoUserProfileStore
import com.example.productivityapp.datastore.profile.SharedPreferencesLegacyProfileReader
import com.example.productivityapp.datastore.profile.UserProfileMigrationCoordinator
import com.example.productivityapp.run.FileProviderRunShareFileWriter
import com.example.productivityapp.run.MapLibreRunMapSnapshotRenderer
import com.example.productivityapp.run.MediaCodecRunReplayVideoEncoder
import com.example.productivityapp.run.RunReplayExporter

object RepositoryProvider {
    private const val ENABLE_SECURE_PROFILE_MIGRATION = true
    private const val ENABLE_SECURE_PROFILE_CUTOVER = true

    @Volatile
    private var appThemeRepository: AppThemeRepository? = null

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

    fun provideWorkoutRepository(context: Context): WorkoutRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomWorkoutRepository(db.workoutDao())
    }

    fun provideMindfulnessRepository(context: Context): MindfulnessRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomMindfulnessRepository(db.mindfulnessDao())
    }

    fun provideRunReplayExporter(context: Context): RunReplayExporter {
        val appContext = context.applicationContext
        return RunReplayExporter(
            context = appContext,
            runRepository = provideRunRepository(appContext),
            snapshotRenderer = MapLibreRunMapSnapshotRenderer(appContext),
            videoEncoder = MediaCodecRunReplayVideoEncoder(),
            shareFileWriter = FileProviderRunShareFileWriter(appContext),
        )
    }

    fun provideSleepRepository(context: Context): SleepRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomSleepRepository(db.sleepDao())
    }

    fun provideWaterRepository(context: Context): WaterRepository {
        val appContext = context.applicationContext
        return DataStoreWaterRepository(
            dataStore = UserDataStore(appContext),
            userProfileRepository = provideUserProfileRepository(appContext),
        )
    }

    fun provideAppThemeRepository(context: Context): AppThemeRepository {
        appThemeRepository?.let { return it }

        return synchronized(this) {
            appThemeRepository?.let { return@synchronized it }

            val appContext = context.applicationContext
            DataStoreAppThemeRepository(
                dataStore = UserDataStore(appContext),
            ).also { appThemeRepository = it }
        }
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

    fun provideUiStateStore(context: Context): com.example.productivityapp.data.UiStateStore {
        return com.example.productivityapp.data.UiStateStore(context.applicationContext)
    }
}
