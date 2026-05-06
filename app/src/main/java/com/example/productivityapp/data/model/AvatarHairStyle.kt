package com.example.productivityapp.data.model

enum class AvatarHairStyle(
    val storageValue: String,
    val label: String,
) {
    SHORT("short", "Short"),
    MEDIUM("medium", "Medium"),
    LONG("long", "Long"),
    CURLY("curly", "Curly"),
    BUN("bun", "Tied Bun"),
    SPIKY("spiky", "Spiky"),

    // New female-oriented styles from provided image
    WAVY_LONG("wavy_long", "Wavy Long"),
    WAVY_SHOULDER("wavy_shoulder", "Wavy Shoulder"),
    CURLY_SHORT("curly_short", "Curly Short"),
    AFRO_SMALL("afro_small", "Afro (Small)"),
    AFRO_LARGE("afro_large", "Afro (Large)"),
    BANGS_STRAIGHT("bangs_straight", "Straight with Bangs"),
    STRAIGHT_LONG("straight_long", "Straight Long"),
    PIXIE_SHORT("pixie_short", "Pixie Short"),
    RED_WAVY_BANGS("red_wavy_bangs", "Wavy Red with Bangs"),
    BRAIDS_LONG("braids_long", "Long Braids");

    companion object {
        fun fromStorageValue(value: String?): AvatarHairStyle {
            return entries.firstOrNull { it.storageValue == value } ?: SHORT
        }
    }
}
