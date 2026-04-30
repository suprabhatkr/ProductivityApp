package com.example.productivityapp.data.repository

import com.example.productivityapp.data.entities.RunPointEntity
import com.example.productivityapp.data.entities.RunEntity
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    fun observeRuns(): Flow<List<RunEntity>>
    fun observeLatestRun(): Flow<RunEntity?>
    fun observeRun(id: Long): Flow<RunEntity?>
    fun observeRunPoints(runId: Long): Flow<List<RunPointEntity>>
    suspend fun getRunById(id: Long): RunEntity?
    suspend fun getRunPoints(runId: Long): List<RunPointEntity>
    suspend fun startRun(run: RunEntity): Long
    suspend fun updateRun(run: RunEntity)
    suspend fun finishRun(runId: Long)
    // Add a location point to an existing run (lat, lon, timestamp ms)
    suspend fun addLocationPoint(point: RunPointEntity)
}
