package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.dao.WorkoutDao
import com.example.productivityapp.data.entities.WorkoutEntity
import com.example.productivityapp.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class RoomWorkoutRepository(
    private val dao: WorkoutDao,
) : WorkoutRepository {
    override fun observeWorkouts(): Flow<List<WorkoutEntity>> = dao.observeAll()

    override fun observeLatestWorkout(): Flow<WorkoutEntity?> = dao.observeLatest()

    override fun observeActiveWorkout(): Flow<WorkoutEntity?> = dao.observeActive()

    override suspend fun getWorkoutById(id: Long): WorkoutEntity? = dao.getById(id)

    override suspend fun getActiveWorkout(): WorkoutEntity? = dao.getActive()

    override suspend fun startWorkout(workout: WorkoutEntity): Long = dao.insert(workout)

    override suspend fun updateWorkout(workout: WorkoutEntity) {
        dao.update(workout)
    }
}
