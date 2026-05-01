package com.example.productivityapp.ui.avatar

import androidx.annotation.DrawableRes
import com.example.productivityapp.R
import com.example.productivityapp.data.model.AvatarGlassesStyle
import com.example.productivityapp.data.model.AvatarHairStyle
import com.example.productivityapp.data.model.AvatarHatStyle
import com.example.productivityapp.data.model.AvatarPresentation

/**
 * Local-first avatar asset catalog for the profile customization flow.
 *
 * Asset rules:
 * - every layer uses the same 512dp viewport
 * - face is rendered first and tinted at runtime for skin tone
 * - presentation overlays stay separate from profile gender
 * - all combinations remain valid; renderer stacking decides the final look
 */
internal object AvatarAssetCatalog {
    @DrawableRes
    val FACE_BASE: Int = R.drawable.avatar_layer_face_base

    @DrawableRes
    val PRESENTATION_LAYERS: List<Int> = listOf(
        R.drawable.avatar_layer_presentation_neutral,
        R.drawable.avatar_layer_presentation_masculine,
        R.drawable.avatar_layer_presentation_feminine,
    )

    @DrawableRes
    val HAIR_LAYERS: List<Int> = listOf(
        R.drawable.avatar_layer_hair_short,
        R.drawable.avatar_layer_hair_medium,
        R.drawable.avatar_layer_hair_long,
        R.drawable.avatar_layer_hair_curly,
        R.drawable.avatar_layer_hair_bun,
        R.drawable.avatar_layer_hair_spiky,
    )

    @DrawableRes
    val GLASSES_LAYERS: List<Int> = listOf(
        R.drawable.avatar_layer_glasses_round,
        R.drawable.avatar_layer_glasses_rect,
        R.drawable.avatar_layer_glasses_bold,
    )

    @DrawableRes
    val HAT_LAYERS: List<Int> = listOf(
        R.drawable.avatar_layer_hat_cap,
        R.drawable.avatar_layer_hat_beanie,
        R.drawable.avatar_layer_hat_sun,
    )

    @DrawableRes
    val ALL_ASSETS: List<Int> = buildList {
        add(FACE_BASE)
        addAll(PRESENTATION_LAYERS)
        addAll(HAIR_LAYERS)
        addAll(GLASSES_LAYERS)
        addAll(HAT_LAYERS)
    }

    @DrawableRes
    fun presentationLayer(presentation: AvatarPresentation): Int = when (presentation) {
        AvatarPresentation.NEUTRAL -> R.drawable.avatar_layer_presentation_neutral
        AvatarPresentation.MASCULINE -> R.drawable.avatar_layer_presentation_masculine
        AvatarPresentation.FEMININE -> R.drawable.avatar_layer_presentation_feminine
    }

    @DrawableRes
    fun hairLayer(hairStyle: AvatarHairStyle): Int = when (hairStyle) {
        AvatarHairStyle.SHORT -> R.drawable.avatar_layer_hair_short
        AvatarHairStyle.MEDIUM -> R.drawable.avatar_layer_hair_medium
        AvatarHairStyle.LONG -> R.drawable.avatar_layer_hair_long
        AvatarHairStyle.CURLY -> R.drawable.avatar_layer_hair_curly
        AvatarHairStyle.BUN -> R.drawable.avatar_layer_hair_bun
        AvatarHairStyle.SPIKY -> R.drawable.avatar_layer_hair_spiky

        // Female-oriented styles mapped to closest existing vector assets as placeholders
        AvatarHairStyle.WAVY_LONG -> R.drawable.avatar_layer_hair_long
        AvatarHairStyle.WAVY_SHOULDER -> R.drawable.avatar_layer_hair_medium
        AvatarHairStyle.CURLY_SHORT -> R.drawable.avatar_layer_hair_curly
        AvatarHairStyle.AFRO_SMALL -> R.drawable.avatar_layer_hair_curly
        AvatarHairStyle.AFRO_LARGE -> R.drawable.avatar_layer_hair_curly
        AvatarHairStyle.BANGS_STRAIGHT -> R.drawable.avatar_layer_hair_medium
        AvatarHairStyle.STRAIGHT_LONG -> R.drawable.avatar_layer_hair_long
        AvatarHairStyle.PIXIE_SHORT -> R.drawable.avatar_layer_hair_short
        AvatarHairStyle.RED_WAVY_BANGS -> R.drawable.avatar_layer_hair_medium
        AvatarHairStyle.BRAIDS_LONG -> R.drawable.avatar_layer_hair_long
    }

    @DrawableRes
    fun glassesLayer(glassesStyle: AvatarGlassesStyle): Int? = when (glassesStyle) {
        AvatarGlassesStyle.NONE -> null
        AvatarGlassesStyle.ROUND -> R.drawable.avatar_layer_glasses_round
        AvatarGlassesStyle.RECTANGULAR -> R.drawable.avatar_layer_glasses_rect
        AvatarGlassesStyle.BOLD -> R.drawable.avatar_layer_glasses_bold
    }

    @DrawableRes
    fun hatLayer(hatStyle: AvatarHatStyle): Int? = when (hatStyle) {
        AvatarHatStyle.NONE -> null
        AvatarHatStyle.CAP -> R.drawable.avatar_layer_hat_cap
        AvatarHatStyle.BEANIE -> R.drawable.avatar_layer_hat_beanie
        AvatarHatStyle.SUN_HAT -> R.drawable.avatar_layer_hat_sun
    }
}
