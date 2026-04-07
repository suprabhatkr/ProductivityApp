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
import com.example.productivityapp.datastore.UserDataStore

object RepositoryProvider {
    fun provideStepRepository(context: Context): StepRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomStepRepository(db.stepDao())
    }

    fun provideRunRepository(context: Context): RunRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomRunRepository(db.runDao())
    }

    fun provideSleepRepository(context: Context): SleepRepository {
        val db = DatabaseProvider.getInstance(context)
        return RoomSleepRepository(db.sleepDao())
    }

    fun provideUserProfileRepository(context: Context): UserProfileRepository {
        val ds = UserDataStore(context.applicationContext)
        return DataStoreUserProfileRepository(ds)
    }
}

