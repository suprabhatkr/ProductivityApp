package com.example.productivityapp.data.model

data class AvatarConfig(
    val avatarId: String = AvatarDefaults.DEFAULT_AVATAR_ID,
) {
    val category: AvatarCategory
        get() = AvatarDefaults.categoryForAvatarId(avatarId)
}
