package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.repository.WaterRepository
import com.example.productivityapp.datastore.UserDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DataStoreWaterRepository(
    private val dataStore: UserDataStore,
    private val userProfileRepository: UserProfileRepository,
) : WaterRepository {

    override fun observeDay(date: String): Flow<WaterDayData> {
        return combine(
            dataStore.observeEntriesForDate(date),
            userProfileRepository.observeUserProfile(),
        ) { entries, profile ->
            WaterDayData(
                date = date,
                entries = entries,
                goalMl = profile.dailyWaterGoalMl,
            )
        }
    }

    override suspend fun addEntry(date: String, amountMl: Int): Long {
        return dataStore.addEntryReturnId(date, amountMl)
    }

    override suspend fun removeEntry(date: String, id: Long) {
        dataStore.removeEntry(date, id)
    }
}
