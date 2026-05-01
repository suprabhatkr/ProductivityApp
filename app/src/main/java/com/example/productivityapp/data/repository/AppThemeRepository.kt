package com.example.productivityapp.data.repository

import com.example.productivityapp.data.model.AppThemePreference
import kotlinx.coroutines.flow.Flow

interface AppThemeRepository {
    fun observeThemePreference(): Flow<AppThemePreference>
    suspend fun updateThemePreference(preference: AppThemePreference)
}
