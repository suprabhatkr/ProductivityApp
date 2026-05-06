package com.example.productivityapp.data.model

enum class AvatarGlassesStyle(
    val storageValue: String,
    val label: String,
) {
    NONE("none", "None"),
    ROUND("round", "Round"),
    RECTANGULAR("rectangular", "Rectangular"),
    BOLD("bold", "Bold Frame");

    companion object {
        fun fromStorageValue(value: String?): AvatarGlassesStyle {
            return entries.firstOrNull { it.storageValue == value } ?: NONE
        }
    }
}
