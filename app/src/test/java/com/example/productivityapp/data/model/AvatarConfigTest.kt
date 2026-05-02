package com.example.productivityapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarConfigTest {

    @Test
    fun avatarConfig_defaultsToCreatureStarterAvatar() {
        val config = AvatarConfig()

        assertEquals(AvatarDefaults.DEFAULT_AVATAR_ID, config.avatarId)
        assertEquals(AvatarCategory.CREATURE, config.category)
    }

    @Test
    fun avatarDefaults_normalizeUnknownIdsToDefaultCreature() {
        assertEquals(AvatarDefaults.DEFAULT_AVATAR_ID, AvatarDefaults.normalizeAvatarId("unknown"))
        assertEquals(AvatarCategory.CREATURE, AvatarDefaults.categoryForAvatarId("unknown"))
    }

    @Test
    fun userProfile_defaultsToDefaultAvatarConfig() {
        assertEquals(AvatarConfig(), UserProfile().avatar)
    }
}
