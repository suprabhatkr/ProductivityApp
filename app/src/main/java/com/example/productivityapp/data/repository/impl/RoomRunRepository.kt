package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.RunDao
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.repository.RunRepository
import kotlinx.coroutines.flow.Flow

class RoomRunRepository(private val dao: RunDao) : RunRepository {
    override fun observeRuns(): Flow<List<RunEntity>> = dao.observeAll()

    override suspend fun getRunById(id: Long): RunEntity? = dao.getById(id)

    override suspend fun startRun(run: RunEntity): Long = dao.insert(run)

    override suspend fun updateRun(run: RunEntity) { dao.update(run) }

    override suspend fun finishRun(runId: Long) {
        // No-op here; caller should update run endTime/duration and call updateRun
    }
}

