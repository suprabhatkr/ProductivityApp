package com.example.productivityapp.app.data.model

import java.time.LocalDateTime

data class WaterEntry(
    val id: Long = System.currentTimeMillis(),
    val amountMl: Int,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class WaterDayData(
    val date: String, // yyyy-MM-dd
    val entries: List<WaterEntry> = emptyList(),
    val goalMl: Int = 2000
) {
    val totalMl: Int get() = entries.sumOf { it.amountMl }
    val progressFraction: Float get() = if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val progressPercent: Int get() = (progressFraction * 100).toInt()
}
