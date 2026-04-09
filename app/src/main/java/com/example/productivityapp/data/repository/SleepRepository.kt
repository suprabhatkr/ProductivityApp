package com.example.productivityapp.data.repository

import com.example.productivityapp.data.entities.SleepEntity
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun observeSleepForDate(date: String): Flow<List<SleepEntity>>
    fun observeSleepForRange(startDate: String, endDate: String): Flow<List<SleepEntity>>
    suspend fun getActiveSleepSession(): SleepEntity?
    suspend fun getSleepById(id: Long): SleepEntity?
    suspend fun startSleep(session: SleepEntity): Long
    suspend fun stopSleep(session: SleepEntity): SleepEntity
    suspend fun updateSleep(session: SleepEntity)
}

