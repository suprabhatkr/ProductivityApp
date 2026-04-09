package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_points",
    indices = [Index(value = ["runId"]), Index(value = ["runId", "tsMs"])]
)
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val lat: Double,
    val lon: Double,
    val tsMs: Long
)

