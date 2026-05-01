package com.example.productivityapp.data.model

enum class AvatarSkinTone(
    val storageValue: String,
    val label: String,
    val swatchHex: String,
) {
    LIGHT("light", "Light", "#F6D7C3"),
    MEDIUM_LIGHT("medium_light", "Medium Light", "#E9BE9A"),
    MEDIUM("medium", "Medium", "#CB9469"),
    MEDIUM_DARK("medium_dark", "Medium Dark", "#9B6846"),
    DARK("dark", "Dark", "#6B452D");

    companion object {
        fun fromStorageValue(value: String?): AvatarSkinTone {
            return entries.firstOrNull { it.storageValue == value } ?: MEDIUM
        }
    }
}
