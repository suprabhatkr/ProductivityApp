package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.StepDao
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.entities.StepSampleEntity
import com.example.productivityapp.data.repository.StepRepository
import kotlinx.coroutines.flow.Flow

class RoomStepRepository(private val dao: StepDao) : StepRepository {
    override fun observeStepsForDate(date: String): Flow<StepEntity?> = dao.observeByDate(date)

    override suspend fun getStepsForDate(date: String): StepEntity? = dao.getByDate(date)

    override suspend fun upsertSteps(step: StepEntity) {
        dao.insert(step)
    }

    override suspend fun incrementSteps(date: String, delta: Int, source: String) {
        val now = System.currentTimeMillis()
        val existing = dao.getByDate(date)
        if (existing == null) {
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
                lastUpdatedAt = now,
                source = source
            )
            dao.update(updated)
        }

        // insert a timestamped sample record for more precise analytics
        try {
            val sample = StepSampleEntity(date = date, tsMs = now, delta = delta, source = source)
            dao.insertSample(sample)
        } catch (_: Throwable) {
            // be resilient if samples aren't available (older DB) or insert fails
        }
    }

    override suspend fun resetStepsForDate(date: String) {
        dao.deleteByDate(date)
        try { dao.deleteSamplesByDate(date) } catch (_: Throwable) {}
    }

    override suspend fun getStepsForRange(startDate: String, endDate: String): List<StepEntity> {
        return dao.getBetweenDates(startDate, endDate)
    }

    override fun observeStepsForRange(startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<StepEntity>> {
        return dao.observeBetweenDates(startDate, endDate)
    }

    override fun observeSamplesForDate(date: String): kotlinx.coroutines.flow.Flow<List<StepSampleEntity>> {
        return dao.observeSamplesByDate(date)
    }

    override suspend fun getSamplesForDate(date: String): List<StepSampleEntity> {
        return dao.getSamplesByDate(date)
    }

    override suspend fun insertSample(sample: StepSampleEntity) {
        dao.insertSample(sample)
    }
}

