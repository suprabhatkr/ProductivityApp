package com.example.productivityapp.data.repository

import com.example.productivityapp.data.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkouts(): Flow<List<WorkoutEntity>>
    fun observeLatestWorkout(): Flow<WorkoutEntity?>
    fun observeActiveWorkout(): Flow<WorkoutEntity?>
    suspend fun getWorkoutById(id: Long): WorkoutEntity?
    suspend fun getActiveWorkout(): WorkoutEntity?
    suspend fun startWorkout(workout: WorkoutEntity): Long
    suspend fun updateWorkout(workout: WorkoutEntity)
}
