package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.productivityapp.data.model.OutdoorActivityType

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityType: String = OutdoorActivityType.RUN.storageValue,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double,
    val durationSec: Long,
    val avgSpeedMps: Double,
    val calories: Double,
    val polyline: String
)

val RunEntity.type: OutdoorActivityType
    get() = OutdoorActivityType.fromStorageValue(activityType)
