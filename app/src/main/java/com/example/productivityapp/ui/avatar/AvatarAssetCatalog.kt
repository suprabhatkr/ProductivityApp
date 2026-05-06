package com.example.productivityapp.ui.avatar

import androidx.annotation.DrawableRes
import com.example.productivityapp.R
import com.example.productivityapp.data.model.AvatarCategory
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarDefaults
import com.example.productivityapp.data.model.AvatarPickerFilter
import kotlin.random.Random

data class AvatarOption(
    val id: String,
    val label: String,
    val category: AvatarCategory,
    @DrawableRes val drawableRes: Int,
)

internal object AvatarAssetCatalog {
    val maleAvatars: List<AvatarOption> = listOf(
        AvatarOption("male_01", "Male 1", AvatarCategory.MALE, R.drawable.avatar_male_01),
        AvatarOption("male_02", "Male 2", AvatarCategory.MALE, R.drawable.avatar_male_02),
        AvatarOption("male_03", "Male 3", AvatarCategory.MALE, R.drawable.avatar_male_03),
        AvatarOption("male_04", "Male 4", AvatarCategory.MALE, R.drawable.avatar_male_04),
        AvatarOption("male_05", "Male 5", AvatarCategory.MALE, R.drawable.avatar_male_05),
        AvatarOption("male_06", "Male 6", AvatarCategory.MALE, R.drawable.avatar_male_06),
        AvatarOption("male_07", "Male 7", AvatarCategory.MALE, R.drawable.avatar_male_07),
        AvatarOption("male_08", "Male 8", AvatarCategory.MALE, R.drawable.avatar_male_08),
        AvatarOption("male_09", "Male 9", AvatarCategory.MALE, R.drawable.avatar_male_09),
        AvatarOption("male_10", "Male 10", AvatarCategory.MALE, R.drawable.avatar_male_10),
        AvatarOption("male_11", "Male 11", AvatarCategory.MALE, R.drawable.avatar_male_11),
        AvatarOption("male_12", "Male 12", AvatarCategory.MALE, R.drawable.avatar_male_12),
        AvatarOption("male_13", "Male 13", AvatarCategory.MALE, R.drawable.avatar_male_13),
        AvatarOption("male_14", "Male 14", AvatarCategory.MALE, R.drawable.avatar_male_14),
        AvatarOption("male_15", "Male 15", AvatarCategory.MALE, R.drawable.avatar_male_15),
        AvatarOption("male_16", "Male 16", AvatarCategory.MALE, R.drawable.avatar_male_16),
        AvatarOption("male_17", "Male 17", AvatarCategory.MALE, R.drawable.avatar_male_17),
        AvatarOption("male_18", "Male 18", AvatarCategory.MALE, R.drawable.avatar_male_18),
        AvatarOption("male_19", "Male 19", AvatarCategory.MALE, R.drawable.avatar_male_19),
        AvatarOption("male_20", "Male 20", AvatarCategory.MALE, R.drawable.avatar_male_20),
        AvatarOption("male_21", "Male 21", AvatarCategory.MALE, R.drawable.avatar_male_21),
        AvatarOption("male_22", "Male 22", AvatarCategory.MALE, R.drawable.avatar_male_22),
        AvatarOption("male_23", "Male 23", AvatarCategory.MALE, R.drawable.avatar_male_23),
        AvatarOption("male_24", "Male 24", AvatarCategory.MALE, R.drawable.avatar_male_24),
    )

    val femaleAvatars: List<AvatarOption> = listOf(
        AvatarOption("female_01", "Female 1", AvatarCategory.FEMALE, R.drawable.avatar_female_01),
        AvatarOption("female_02", "Female 2", AvatarCategory.FEMALE, R.drawable.avatar_female_02),
        AvatarOption("female_03", "Female 3", AvatarCategory.FEMALE, R.drawable.avatar_female_03),
        AvatarOption("female_04", "Female 4", AvatarCategory.FEMALE, R.drawable.avatar_female_04),
        AvatarOption("female_05", "Female 5", AvatarCategory.FEMALE, R.drawable.avatar_female_05),
        AvatarOption("female_06", "Female 6", AvatarCategory.FEMALE, R.drawable.avatar_female_06),
        AvatarOption("female_07", "Female 7", AvatarCategory.FEMALE, R.drawable.avatar_female_07),
        AvatarOption("female_08", "Female 8", AvatarCategory.FEMALE, R.drawable.avatar_female_08),
        AvatarOption("female_09", "Female 9", AvatarCategory.FEMALE, R.drawable.avatar_female_09),
        AvatarOption("female_10", "Female 10", AvatarCategory.FEMALE, R.drawable.avatar_female_10),
        AvatarOption("female_11", "Female 11", AvatarCategory.FEMALE, R.drawable.avatar_female_11),
        AvatarOption("female_12", "Female 12", AvatarCategory.FEMALE, R.drawable.avatar_female_12),
        AvatarOption("female_13", "Female 13", AvatarCategory.FEMALE, R.drawable.avatar_female_13),
        AvatarOption("female_14", "Female 14", AvatarCategory.FEMALE, R.drawable.avatar_female_14),
        AvatarOption("female_15", "Female 15", AvatarCategory.FEMALE, R.drawable.avatar_female_15),
        AvatarOption("female_16", "Female 16", AvatarCategory.FEMALE, R.drawable.avatar_female_16),
        AvatarOption("female_17", "Female 17", AvatarCategory.FEMALE, R.drawable.avatar_female_17),
        AvatarOption("female_18", "Female 18", AvatarCategory.FEMALE, R.drawable.avatar_female_18),
        AvatarOption("female_19", "Female 19", AvatarCategory.FEMALE, R.drawable.avatar_female_19),
        AvatarOption("female_20", "Female 20", AvatarCategory.FEMALE, R.drawable.avatar_female_20),
        AvatarOption("female_21", "Female 21", AvatarCategory.FEMALE, R.drawable.avatar_female_21),
        AvatarOption("female_22", "Female 22", AvatarCategory.FEMALE, R.drawable.avatar_female_22),
        AvatarOption("female_23", "Female 23", AvatarCategory.FEMALE, R.drawable.avatar_female_23),
        AvatarOption("female_24", "Female 24", AvatarCategory.FEMALE, R.drawable.avatar_female_24),
    )

    val creatureAvatars: List<AvatarOption> = listOf(
        AvatarOption("creature_01", "Creature 1", AvatarCategory.CREATURE, R.drawable.avatar_creature_01),
        AvatarOption("creature_02", "Creature 2", AvatarCategory.CREATURE, R.drawable.avatar_creature_02),
        AvatarOption("creature_03", "Creature 3", AvatarCategory.CREATURE, R.drawable.avatar_creature_03),
        AvatarOption("creature_04", "Creature 4", AvatarCategory.CREATURE, R.drawable.avatar_creature_04),
        AvatarOption("creature_05", "Creature 5", AvatarCategory.CREATURE, R.drawable.avatar_creature_05),
        AvatarOption("creature_06", "Creature 6", AvatarCategory.CREATURE, R.drawable.avatar_creature_06),
    )

    val allAvatars: List<AvatarOption> = maleAvatars + femaleAvatars + creatureAvatars

    private val avatarsById: Map<String, AvatarOption> = allAvatars.associateBy(AvatarOption::id)

    val defaultAvatar: AvatarOption = avatarsById.getValue(AvatarDefaults.DEFAULT_AVATAR_ID)

    fun optionFor(avatar: AvatarConfig): AvatarOption = optionForId(avatar.avatarId)

    fun optionForId(avatarId: String?): AvatarOption {
        return avatarsById[AvatarDefaults.normalizeAvatarId(avatarId)] ?: defaultAvatar
    }

    fun optionsFor(
        filter: AvatarPickerFilter,
        shuffleSeed: Int = 0,
    ): List<AvatarOption> {
        return when (filter) {
            AvatarPickerFilter.GENERAL -> allAvatars.shuffled(Random(shuffleSeed.toLong() + 31L))
            AvatarPickerFilter.MALE -> maleAvatars
            AvatarPickerFilter.FEMALE -> femaleAvatars
            AvatarPickerFilter.CREATURE -> creatureAvatars
        }
    }
}
