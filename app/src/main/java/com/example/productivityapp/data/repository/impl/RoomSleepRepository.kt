package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.SleepDao
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.flow.Flow

class RoomSleepRepository(private val dao: SleepDao) : SleepRepository {
    override fun observeSleepForDate(date: String): Flow<List<SleepEntity>> = dao.observeForDate(date)

    override suspend fun startSleep(session: SleepEntity): Long = dao.insert(session)

    override suspend fun stopSleep(session: SleepEntity): SleepEntity {
        dao.update(session)
        return session
    }

    override suspend fun updateSleep(session: SleepEntity) { dao.update(session) }
}

