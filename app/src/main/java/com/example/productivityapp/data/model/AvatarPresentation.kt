package com.example.productivityapp.data.model

enum class AvatarPresentation(
    val storageValue: String,
    val label: String,
) {
    NEUTRAL("neutral", "Neutral"),
    MASCULINE("masculine", "Masculine"),
    FEMININE("feminine", "Feminine");

    companion object {
        fun fromStorageValue(value: String?): AvatarPresentation {
            return entries.firstOrNull { it.storageValue == value } ?: NEUTRAL
        }
    }
}
