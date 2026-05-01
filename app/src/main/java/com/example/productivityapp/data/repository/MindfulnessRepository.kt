package com.example.productivityapp.data.repository

import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import kotlinx.coroutines.flow.Flow

interface MindfulnessRepository {
    fun observeSessions(): Flow<List<MindfulnessSessionEntity>>
    fun observeLogs(): Flow<List<MindLogEntity>>
    fun observeActiveSession(): Flow<MindfulnessSessionEntity?>
    suspend fun getSessionById(id: Long): MindfulnessSessionEntity?
    suspend fun startSession(session: MindfulnessSessionEntity): Long
    suspend fun updateSession(session: MindfulnessSessionEntity)
    suspend fun addLog(log: MindLogEntity): Long
}
