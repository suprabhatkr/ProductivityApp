package com.example.productivityapp.data.repository

import com.example.productivityapp.data.entities.RunEntity
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    fun observeRuns(): Flow<List<RunEntity>>
    suspend fun getRunById(id: Long): RunEntity?
    suspend fun startRun(run: RunEntity): Long
    suspend fun updateRun(run: RunEntity)
    suspend fun finishRun(runId: Long)
}

