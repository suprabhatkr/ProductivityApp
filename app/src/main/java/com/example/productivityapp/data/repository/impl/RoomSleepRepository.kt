package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.SleepDao
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.flow.Flow

class RoomSleepRepository(private val dao: SleepDao) : SleepRepository {
    override fun observeSleepForDate(date: String): Flow<List<SleepEntity>> = dao.observeForDate(date)

    override fun observeSleepForRange(startDate: String, endDate: String): Flow<List<SleepEntity>> =
        dao.observeForDateRange(startDate, endDate)

    override suspend fun getActiveSleepSession(): SleepEntity? = dao.getActiveSession()

    override suspend fun getSleepById(id: Long): SleepEntity? = dao.getById(id)

    override suspend fun startSleep(session: SleepEntity): Long {
        val active = dao.getActiveSession()
        return if (active != null) active.id else dao.insert(session)
    }

    override suspend fun stopSleep(session: SleepEntity): SleepEntity {
        dao.update(session)
        return session
    }

    override suspend fun updateSleep(session: SleepEntity) { dao.update(session) }
}

