package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double,
    val durationSec: Long,
    val avgSpeedMps: Double,
    val calories: Double,
    val polyline: String
)

