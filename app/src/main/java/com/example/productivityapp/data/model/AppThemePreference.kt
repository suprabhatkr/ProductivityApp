package com.example.productivityapp.data.model

enum class AppThemePreference(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): AppThemePreference = when (value) {
            LIGHT.storageValue -> LIGHT
            DARK.storageValue -> DARK
            else -> SYSTEM
        }
    }
}
