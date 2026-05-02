package com.example.productivityapp.ui.avatar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.productivityapp.data.model.AvatarConfig
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
                    avatar = AvatarConfig(avatarId = "female_04"),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            "Avatar preview, Female 4, female style",
        ).assertIsDisplayed()
    }

    @Test
    fun avatarOptionCard_invokesClickHandler() {
        var clicked = false

        composeRule.setContent {
            MaterialTheme {
                AvatarOptionCard(
                    option = AvatarAssetCatalog.optionForId("male_01"),
                    selected = true,
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Male 1 avatar option")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }
}
