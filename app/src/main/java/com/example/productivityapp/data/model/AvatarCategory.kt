package com.example.productivityapp.data.model

enum class AvatarCategory(
    val storageValue: String,
    val label: String,
) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    CREATURE("creature", "Creature");

    companion object {
        fun fromStorageValue(value: String?): AvatarCategory {
            return entries.firstOrNull { it.storageValue == value } ?: CREATURE
        }
    }
}
