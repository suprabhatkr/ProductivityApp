package com.example.productivityapp.data.repository

import com.example.productivityapp.data.entities.StepEntity
import kotlinx.coroutines.flow.Flow

interface StepRepository {
    fun observeStepsForDate(date: String): Flow<StepEntity?>
    suspend fun getStepsForDate(date: String): StepEntity?
    suspend fun getStepsForRange(startDate: String, endDate: String): List<StepEntity>
    fun observeStepsForRange(startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<StepEntity>>
    suspend fun upsertSteps(step: StepEntity)
    suspend fun incrementSteps(date: String, delta: Int, source: String = "sensor")
    suspend fun resetStepsForDate(date: String)
}

