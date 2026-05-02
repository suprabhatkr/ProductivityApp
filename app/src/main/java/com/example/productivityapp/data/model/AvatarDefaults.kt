package com.example.productivityapp.data.model

object AvatarDefaults {
    val maleAvatarIds: List<String> = (1..24).map { "male_${it.toString().padStart(2, '0')}" }
    val femaleAvatarIds: List<String> = (1..24).map { "female_${it.toString().padStart(2, '0')}" }
    val creatureAvatarIds: List<String> = (1..6).map { "creature_${it.toString().padStart(2, '0')}" }

    val allAvatarIds: List<String> = maleAvatarIds + femaleAvatarIds + creatureAvatarIds

    const val DEFAULT_AVATAR_ID: String = "creature_01"

    fun normalizeAvatarId(value: String?): String {
        return value?.takeIf(allAvatarIds::contains) ?: DEFAULT_AVATAR_ID
    }

    fun categoryForAvatarId(value: String?): AvatarCategory {
        val normalized = normalizeAvatarId(value)
        return when (normalized) {
            in maleAvatarIds -> AvatarCategory.MALE
            in femaleAvatarIds -> AvatarCategory.FEMALE
            else -> AvatarCategory.CREATURE
        }
    }

    fun fallbackIdForLegacyPresentation(presentation: AvatarPresentation?): String {
        return when (presentation) {
            AvatarPresentation.MASCULINE -> maleAvatarIds.first()
            AvatarPresentation.FEMININE -> femaleAvatarIds.first()
            AvatarPresentation.NEUTRAL,
            null,
            -> DEFAULT_AVATAR_ID
        }
    }
}
