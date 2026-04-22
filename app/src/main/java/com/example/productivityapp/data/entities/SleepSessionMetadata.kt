package com.example.productivityapp.data.entities

enum class SleepDetectionSource(val storageValue: String) {
    MANUAL("manual"),
    AUTO("auto"),
    NAP("nap");

    companion object {
        fun fromStorageValue(value: String?): SleepDetectionSource {
            return values().firstOrNull { it.storageValue == value } ?: MANUAL
        }
    }
}

enum class SleepReviewState(val storageValue: String) {
    CONFIRMED("confirmed"),
    NEEDS_REVIEW("needs_review"),
    PROVISIONAL("provisional"),
    DISMISSED("dismissed");

    companion object {
        fun fromStorageValue(value: String?): SleepReviewState {
            return values().firstOrNull { it.storageValue == value } ?: CONFIRMED
        }
    }
}

fun String?.toSleepTags(): List<String> {
    return this
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
}

fun List<String>.toSleepTagsStorage(): String? {
    val cleaned = map { it.trim() }.filter { it.isNotBlank() }
    return cleaned.takeIf { it.isNotEmpty() }?.joinToString(",")
}
