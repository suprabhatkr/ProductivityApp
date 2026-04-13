package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Timestamped step delta samples flushed by the StepCounterService. Each row records the
 * number of steps observed (delta) at a specific tsMs and associated date (ISO yyyy-MM-dd)
 * to make date-bounded queries simple.
 */
@Entity(
    tableName = "step_samples",
    indices = [Index(value = ["date"])]
)
data class StepSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val tsMs: Long,
    val delta: Int,
    val source: String
)
