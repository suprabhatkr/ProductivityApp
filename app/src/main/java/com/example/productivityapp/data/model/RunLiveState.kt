package com.example.productivityapp.data.model

data class ActiveRunSession(
    val runId: Long,
    val isPaused: Boolean,
    val distanceMeters: Double,
    val durationSec: Long,
    val avgSpeedMps: Double,
)
