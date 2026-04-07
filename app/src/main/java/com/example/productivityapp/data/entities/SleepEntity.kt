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
    val notes: String?
)

