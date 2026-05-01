package com.example.productivityapp.ui.avatar

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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.semantics.Role
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarSkinTone

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
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .padding((size * 0.08f).coerceAtLeast(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AvatarLayer(
            drawableRes = AvatarAssetCatalog.FACE_BASE,
            modifier = Modifier.fillMaxSize(),
            tint = AvatarPreviewDefaults.skinToneColor(avatar.skinTone),
        )
        AvatarLayer(
            drawableRes = AvatarAssetCatalog.presentationLayer(avatar.presentation),
            modifier = Modifier.fillMaxSize(),
        )
        AvatarLayer(
            drawableRes = AvatarAssetCatalog.hairLayer(avatar.hairStyle),
            modifier = Modifier.fillMaxSize(),
        )
        AvatarAssetCatalog.glassesLayer(avatar.glassesStyle)?.let { drawableRes ->
            AvatarLayer(
                drawableRes = drawableRes,
                modifier = Modifier.fillMaxSize(),
            )
        }
        AvatarAssetCatalog.hatLayer(avatar.hatStyle)?.let { drawableRes ->
            AvatarLayer(
                drawableRes = drawableRes,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoxScope.AvatarLayer(
    drawableRes: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        modifier = modifier.align(Alignment.Center),
        contentScale = ContentScale.Fit,
        colorFilter = tint?.let(ColorFilter::tint),
    )
}

@Composable
fun AvatarTraitOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    preview: @Composable () -> Unit,
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
                contentDescription = "$label avatar option"
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    preview()
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
fun AvatarSkinToneOptionCard(
    skinTone: AvatarSkinTone,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AvatarTraitOptionCard(
        label = skinTone.label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AvatarPreviewDefaults.skinToneColor(skinTone))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
        )
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

    fun skinToneColor(skinTone: AvatarSkinTone): Color = when (skinTone) {
        AvatarSkinTone.LIGHT -> Color(0xFFF6D7C3)
        AvatarSkinTone.MEDIUM_LIGHT -> Color(0xFFE9BE9A)
        AvatarSkinTone.MEDIUM -> Color(0xFFCB9469)
        AvatarSkinTone.MEDIUM_DARK -> Color(0xFF9B6846)
        AvatarSkinTone.DARK -> Color(0xFF6B452D)
    }
}

internal fun avatarContentDescription(avatar: AvatarConfig): String {
    return buildString {
        append("Avatar preview, ")
        append(avatar.skinTone.label)
        append(" skin, ")
        append(avatar.presentation.label.lowercase())
        append(" look, ")
        append(avatar.hairStyle.label.lowercase())
        append(" hair, ")
        append(avatar.glassesStyle.label.lowercase())
        append(" glasses, ")
        append(avatar.hatStyle.label.lowercase())
        append(" hat")
    }
}
