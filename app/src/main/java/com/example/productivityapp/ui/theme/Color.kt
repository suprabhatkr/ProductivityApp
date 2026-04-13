package com.example.productivityapp.ui.theme

import androidx.compose.ui.graphics.Color

// Foundation
val CreamBackground = Color(0xFFFAF4EC)
val SurfaceWhite = Color(0xFFFFFAF5)
val TrackBeige = Color(0xFFF0EBE1)

// Accent palette
val AccentYellow = Color(0xFFF4B942)
val AccentTerracotta = Color(0xFFE76F51)
val MidToneAccent = Color(0xFFF4A261)

// Blend (80% yellow + 20% terracotta)
val BlendPrimary = Color(0xFFF1AA45)
val BlendPrimaryDark = Color(0xFF906629)
val BlendDarkBackground = Color(0xFF171513)
val BlendLight = Color(0xFFF7D7B1)

// Text tones
val TextPrimary = Color(0xFF2D2825)
val TextSecondary = Color(0xFF8A817C)

// Activity & trend
val PositiveTrend = Color(0xFFD95A40)
val NightEvening = Color(0xFF4A5568)

// Compatibility aliases and helpers
val StepsAmber = AccentYellow
val StepsAmberDark = AccentTerracotta
val StepsAmberSoft = Color(0xFFFBE9A7)

// Legacy / other colors kept for compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Per-feature accents (legacy names kept)
val WaterBlue = Color(0xFF2196F3)
val SleepGreen = Color(0xFF4CAF50)
val RunPurple = Color(0xFFB155E0)

// General dark backgrounds used in steps screens (deprecated)
val StepsBackground = BlendDarkBackground
val StepsSurface = Color(0xFF211E1B)
val OnStepsSurface = Color(0xFFFFFFFF)

// Hex constants for non-compose usages (OSMdroid overlays etc.)
const val RUN_ACCENT_HEX = "#B155E0"
