package com.example.productivityapp.data.repository

import com.example.productivityapp.app.data.model.WaterDayData
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun observeDay(date: String): Flow<WaterDayData>
    suspend fun addEntry(date: String, amountMl: Int): Long
    suspend fun removeEntry(date: String, id: Long)
}
