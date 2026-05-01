package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.model.AppThemePreference
import com.example.productivityapp.data.repository.AppThemeRepository
import com.example.productivityapp.datastore.UserDataStore
import kotlinx.coroutines.flow.Flow

class DataStoreAppThemeRepository(
    private val dataStore: UserDataStore,
) : AppThemeRepository {
    override fun observeThemePreference(): Flow<AppThemePreference> = dataStore.observeThemePreference()

    override suspend fun updateThemePreference(preference: AppThemePreference) {
        dataStore.updateThemePreference(preference)
    }
}
