package com.example.productivityapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarConfigTest {

    @Test
    fun avatarConfig_defaultsMatchPlannedBaseline() {
        val config = AvatarConfig()

        assertEquals(AvatarSkinTone.MEDIUM, config.skinTone)
        assertEquals(AvatarPresentation.NEUTRAL, config.presentation)
        assertEquals(AvatarHairStyle.SHORT, config.hairStyle)
        assertEquals(AvatarGlassesStyle.NONE, config.glassesStyle)
        assertEquals(AvatarHatStyle.NONE, config.hatStyle)
    }

    @Test
    fun avatarEnums_fallBackToStableDefaultsForUnknownStorageValues() {
        assertEquals(AvatarSkinTone.MEDIUM, AvatarSkinTone.fromStorageValue("unknown"))
        assertEquals(AvatarPresentation.NEUTRAL, AvatarPresentation.fromStorageValue("unknown"))
        assertEquals(AvatarHairStyle.SHORT, AvatarHairStyle.fromStorageValue("unknown"))
        assertEquals(AvatarGlassesStyle.NONE, AvatarGlassesStyle.fromStorageValue("unknown"))
        assertEquals(AvatarHatStyle.NONE, AvatarHatStyle.fromStorageValue("unknown"))
    }

    @Test
    fun userProfile_defaultsToDefaultAvatarConfig() {
        assertEquals(AvatarConfig(), UserProfile().avatar)
    }
}
