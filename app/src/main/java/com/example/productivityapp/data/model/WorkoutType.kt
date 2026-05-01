package com.example.productivityapp.data.model

enum class WorkoutType(
    val storageValue: String,
    val label: String,
) {
    INDOOR("indoor", "Indoor"),
    OUTDOOR("outdoor", "Outdoor"),
    INTENSE("intense_workout", "Intense Workout"),
    YOGA("yoga", "Yoga"),
    LIGHT("light_workout", "Light Workout"),
    PUSHUPS("pushups", "Pushups"),
    OTHER("other", "Other");

    companion object {
        fun fromStorageValue(value: String?): WorkoutType {
            return entries.firstOrNull { it.storageValue == value } ?: INDOOR
        }
    }
}
