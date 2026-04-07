package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.StepDao
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.repository.StepRepository
import kotlinx.coroutines.flow.Flow

class RoomStepRepository(private val dao: StepDao) : StepRepository {
    override fun observeStepsForDate(date: String): Flow<StepEntity?> = dao.observeByDate(date)

    override suspend fun getStepsForDate(date: String): StepEntity? = dao.getByDate(date)

    override suspend fun upsertSteps(step: StepEntity) {
        dao.insert(step)
    }

    override suspend fun incrementSteps(date: String, delta: Int, source: String) {
        val existing = dao.getByDate(date)
        if (existing == null) {
            val now = System.currentTimeMillis()
            dao.insert(
                StepEntity(
                    date = date,
                    steps = delta,
                    distanceMeters = 0.0,
                    calories = 0.0,
                    source = source,
                    lastUpdatedAt = now
                )
            )
        } else {
            val updated = existing.copy(
                steps = existing.steps + delta,
                lastUpdatedAt = System.currentTimeMillis(),
                source = source
            )
            dao.update(updated)
        }
    }

    override suspend fun resetStepsForDate(date: String) {
        dao.deleteByDate(date)
    }
}

