package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.productivityapp.data.model.WorkoutType

@Entity(
    tableName = "workouts",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutType: String,
    val startTime: Long,
    val endTime: Long?,
    val durationSec: Long,
    val notes: String? = null,
)

val WorkoutEntity.type: WorkoutType
    get() = WorkoutType.fromStorageValue(workoutType)
