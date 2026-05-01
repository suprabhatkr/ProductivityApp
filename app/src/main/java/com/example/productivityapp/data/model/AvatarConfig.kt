package com.example.productivityapp.data.model

data class AvatarConfig(
    val skinTone: AvatarSkinTone = AvatarSkinTone.MEDIUM,
    val presentation: AvatarPresentation = AvatarPresentation.NEUTRAL,
    val hairStyle: AvatarHairStyle = AvatarHairStyle.SHORT,
    val glassesStyle: AvatarGlassesStyle = AvatarGlassesStyle.NONE,
    val hatStyle: AvatarHatStyle = AvatarHatStyle.NONE,
)
