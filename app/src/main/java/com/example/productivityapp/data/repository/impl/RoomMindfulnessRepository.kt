package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.MindfulnessDao
import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import com.example.productivityapp.data.repository.MindfulnessRepository
import kotlinx.coroutines.flow.Flow

class RoomMindfulnessRepository(
    private val dao: MindfulnessDao,
) : MindfulnessRepository {
    override fun observeSessions(): Flow<List<MindfulnessSessionEntity>> = dao.observeSessions()

    override fun observeLogs(): Flow<List<MindLogEntity>> = dao.observeLogs()

    override fun observeActiveSession(): Flow<MindfulnessSessionEntity?> = dao.observeActiveSession()

    override suspend fun getSessionById(id: Long): MindfulnessSessionEntity? = dao.getSessionById(id)

    override suspend fun startSession(session: MindfulnessSessionEntity): Long = dao.insertSession(session)

    override suspend fun updateSession(session: MindfulnessSessionEntity) {
        dao.updateSession(session)
    }

    override suspend fun addLog(log: MindLogEntity): Long = dao.insertLog(log)
}
