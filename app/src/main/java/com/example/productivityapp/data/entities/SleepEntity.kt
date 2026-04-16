package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleeps")
data class SleepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSec: Long,
    val sleepQuality: Int?,
    val notes: String?,
    val detectionSource: String = SleepDetectionSource.MANUAL.storageValue,
    val confidenceScore: Double = 1.0,
    val inferredStartTimestamp: Long? = null,
    val inferredEndTimestamp: Long? = null,
    val reviewState: String = SleepReviewState.CONFIRMED.storageValue,
    val tagsCsv: String? = null,
)

val SleepEntity.tags: List<String>
    get() = tagsCsv.toSleepTags()
