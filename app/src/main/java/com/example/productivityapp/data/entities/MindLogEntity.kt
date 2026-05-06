package com.example.productivityapp.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mind_logs",
    indices = [Index(value = ["createdAt"])],
)
data class MindLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val content: String,
)
