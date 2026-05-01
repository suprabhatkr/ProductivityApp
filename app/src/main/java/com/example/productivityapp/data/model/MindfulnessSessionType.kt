package com.example.productivityapp.data.model

enum class MindfulnessSessionType(
    val storageValue: String,
    val label: String,
) {
    BREATHING("breathing", "Breathing"),
    MEDITATION("meditation", "Meditation");

    companion object {
        fun fromStorageValue(value: String?): MindfulnessSessionType {
            return entries.firstOrNull { it.storageValue == value } ?: BREATHING
        }
    }
}
