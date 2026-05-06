package com.example.productivityapp.data.model

enum class AvatarHatStyle(
    val storageValue: String,
    val label: String,
) {
    NONE("none", "None"),
    CAP("cap", "Cap"),
    BEANIE("beanie", "Beanie"),
    SUN_HAT("sun_hat", "Sun Hat");

    companion object {
        fun fromStorageValue(value: String?): AvatarHatStyle {
            return entries.firstOrNull { it.storageValue == value } ?: NONE
        }
    }
}
