package com.example.productivityapp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue700 = Color(0xFF1565C0)
val Blue500 = Color(0xFF1976D2)
val Blue200 = Color(0xFF90CAF9)
val Blue50  = Color(0xFFE3F2FD)
val BlueLight = Color(0xFF64B5F6)

val Surface = Color(0xFFF3F4F6)
val CardBg  = Color(0xFFFFFFFF)
val DividerColor = Color(0xFFE5E7EB)
val TextPrimary   = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary  = Color(0xFF9CA3AF)

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue700,
    secondary = Blue500,
    background = Surface,
    surface = CardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = DividerColor
)

@Composable
fun ProductivityAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
