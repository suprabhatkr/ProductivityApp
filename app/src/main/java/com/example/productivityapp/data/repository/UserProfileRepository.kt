package com.example.productivityapp.data.repository

import com.example.productivityapp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeUserProfile(): Flow<UserProfile>
    fun getUserProfileBlocking(): UserProfile
    suspend fun updateUserProfile(profile: UserProfile)
}

