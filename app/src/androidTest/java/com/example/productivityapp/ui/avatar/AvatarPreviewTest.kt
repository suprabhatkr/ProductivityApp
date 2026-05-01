package com.example.productivityapp.ui.avatar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarGlassesStyle
import com.example.productivityapp.data.model.AvatarHairStyle
import com.example.productivityapp.data.model.AvatarHatStyle
import com.example.productivityapp.data.model.AvatarPresentation
import com.example.productivityapp.data.model.AvatarSkinTone
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AvatarPreviewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun avatarPreview_rendersWithAccessibleDescription() {
        composeRule.setContent {
            MaterialTheme {
                AvatarPreview(
                    avatar = AvatarConfig(
                        skinTone = AvatarSkinTone.MEDIUM_DARK,
                        presentation = AvatarPresentation.FEMININE,
                        hairStyle = AvatarHairStyle.CURLY,
                        glassesStyle = AvatarGlassesStyle.ROUND,
                        hatStyle = AvatarHatStyle.BEANIE,
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            "Avatar preview, Medium Dark skin, feminine look, curly hair, round glasses, beanie hat",
        ).assertIsDisplayed()
    }

    @Test
    fun avatarTraitOptionCard_invokesClickHandler() {
        var clicked = false

        composeRule.setContent {
            MaterialTheme {
                AvatarTraitOptionCard(
                    label = "Curly",
                    selected = true,
                    onClick = { clicked = true },
                ) {
                    AvatarPreview(
                        avatar = AvatarConfig(hairStyle = AvatarHairStyle.CURLY),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Curly avatar option").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }
}
