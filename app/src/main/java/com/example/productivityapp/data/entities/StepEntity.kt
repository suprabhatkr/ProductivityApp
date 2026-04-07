package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps")
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val steps: Int,
    val distanceMeters: Double,
    val calories: Double,
    val source: String,
    val lastUpdatedAt: Long
)

