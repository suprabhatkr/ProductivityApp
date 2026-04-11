package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.datastore.UserDataStore
import kotlinx.coroutines.flow.Flow

class DataStoreUserProfileRepository(private val dataStore: UserDataStore) : UserProfileRepository {
    override fun observeUserProfile(): Flow<UserProfile> = dataStore.observeUserProfile()

    override fun getUserProfileBlocking(): UserProfile = dataStore.getUserProfileBlocking()

    override suspend fun updateUserProfile(profile: UserProfile) {
        dataStore.updateUserProfile(profile)
    }
}

