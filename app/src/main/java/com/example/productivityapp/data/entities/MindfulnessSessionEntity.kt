package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.productivityapp.data.model.MindfulnessSessionType

@Entity(
    tableName = "mindfulness_sessions",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
    ],
)
data class MindfulnessSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionType: String,
    val startTime: Long,
    val endTime: Long?,
    val durationSec: Long,
    val note: String? = null,
)

val MindfulnessSessionEntity.type: MindfulnessSessionType
    get() = MindfulnessSessionType.fromStorageValue(sessionType)
