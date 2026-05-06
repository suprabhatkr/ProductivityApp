package com.example.productivityapp.ui.avatar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.productivityapp.data.model.AvatarConfig

@Composable
fun AvatarPreview(
    avatar: AvatarConfig,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    containerColor: Color = AvatarPreviewDefaults.containerColor(),
    borderColor: Color = AvatarPreviewDefaults.borderColor(),
    contentDescription: String? = avatarContentDescription(avatar),
    onClick: (() -> Unit)? = null,
) {
    val option = AvatarAssetCatalog.optionFor(avatar)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        if (onClick != null) role = Role.Button
                    }
                } else {
                    Modifier
                }
            )
            .then(clickableModifier)
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = option.drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun AvatarOptionCard(
    option: AvatarOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    Card(
        modifier = modifier
            .semantics {
                contentDescription = "${option.label} avatar option"
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = AvatarPreviewDefaults.containerColor(),
                shape = RoundedCornerShape(18.dp),
            ) {
                AvatarPreview(
                    avatar = AvatarConfig(option.id),
                    size = 88.dp,
                    modifier = Modifier.padding(8.dp),
                    contentDescription = null,
                )
            }
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

object AvatarPreviewDefaults {
    @Composable
    fun containerColor(): Color {
        val scheme = MaterialTheme.colorScheme
        return if (scheme.surface.luminance() < 0.5f) {
            scheme.surfaceVariant.copy(alpha = 0.88f)
        } else {
            scheme.surface.copy(alpha = 0.96f)
        }
    }

    @Composable
    fun borderColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
}

internal fun avatarContentDescription(avatar: AvatarConfig): String {
    val option = AvatarAssetCatalog.optionFor(avatar)
    return "Avatar preview, ${option.label}, ${option.category.label.lowercase()} style"
}
