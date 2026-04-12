package com.example.productivityapp.app.ui.legacy

/**
 * Legacy sealed route model (moved to the legacy package). Retained for historical
 * context. Active navigation uses `com.example.productivityapp.navigation.AppRoutes`.
 */
@Deprecated(
    message = "Legacy route model. Use com.example.productivityapp.navigation.AppRoutes instead.",
    replaceWith = ReplaceWith("com.example.productivityapp.navigation.AppRoutes"),
    level = DeprecationLevel.WARNING
)
sealed class Screen {
    object Home : Screen()
    object WaterIntake : Screen()
    object Steps : Screen()
    object Run : Screen()
    object Sleep : Screen()
    object Settings : Screen()
}

