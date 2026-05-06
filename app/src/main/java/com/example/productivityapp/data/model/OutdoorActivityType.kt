package com.example.productivityapp.data.model

enum class OutdoorActivityType(
    val storageValue: String,
    val label: String,
) {
    RUN("run", "Run"),
    WALK("walk", "Walk");

    companion object {
        fun fromStorageValue(value: String?): OutdoorActivityType {
            return entries.firstOrNull { it.storageValue == value } ?: RUN
        }
    }
}
