package com.example.productivityapp.data.model

data class UserProfile(
    val displayName: String? = null,
    val weightKg: Double? = null,
    val heightCm: Int? = null,
    val strideLengthMeters: Double = 0.78,
    val preferredUnits: String = "metric",
    val dailyStepGoal: Int = 10000,
    val dailyWaterGoalMl: Int = 2000,
    val nightlySleepGoalMinutes: Int = 480,
    val typicalBedtimeMinutes: Int = 1320,
    val typicalWakeTimeMinutes: Int = 420,
    val sleepDetectionBufferMinutes: Int = 30
)
