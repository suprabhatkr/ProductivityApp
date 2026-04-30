package com.example.productivityapp.ui.run

internal enum class RunPrimaryAction {
    REQUEST_LOCATION,
    START_OR_RESUME_RUN,
    STOP_RUN,
}

internal enum class RunPermissionCardAction {
    REQUEST_LOCATION,
    REQUEST_NOTIFICATIONS,
    REQUEST_BACKGROUND,
    OPEN_BACKGROUND_SETTINGS,
}

internal data class RunPermissionCardModel(
    val title: String,
    val message: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val action: RunPermissionCardAction,
)

internal data class RunPermissionUiState(
    val primaryActionLabel: String,
    val primaryAction: RunPrimaryAction,
    val permissionCard: RunPermissionCardModel?,
    val shouldRequestNotificationsBeforeTracking: Boolean,
)

internal data class RunPermissionContext(
    val hasLocationPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val hasBackgroundPermission: Boolean,
    val shouldShowBackgroundRationale: Boolean,
    val shouldPromptNotificationPermission: Boolean,
    val shouldShowBackgroundPermissionCard: Boolean,
    val shouldOpenBackgroundSettings: Boolean,
    val isTracking: Boolean,
    val hasPausedRun: Boolean,
)

internal class RunPermissionCoordinator {

    fun buildUiState(context: RunPermissionContext): RunPermissionUiState {
        val primaryAction = when {
            !context.hasLocationPermission -> RunPrimaryAction.REQUEST_LOCATION
            context.isTracking -> RunPrimaryAction.STOP_RUN
            else -> RunPrimaryAction.START_OR_RESUME_RUN
        }
        val primaryLabel = when (primaryAction) {
            RunPrimaryAction.REQUEST_LOCATION -> "Enable Location"
            RunPrimaryAction.START_OR_RESUME_RUN -> "Start Run"
            RunPrimaryAction.STOP_RUN -> "Stop Run"
        }

        val permissionCard = when {
            !context.hasLocationPermission -> RunPermissionCardModel(
                title = "Enable precise location",
                message = "Ask for location only when you want to start a run. It powers route mapping, pace, and distance.",
                primaryLabel = "Grant location",
                secondaryLabel = "App settings",
                action = RunPermissionCardAction.REQUEST_LOCATION,
            )

            context.isTracking &&
                context.shouldPromptNotificationPermission &&
                !context.hasNotificationPermission -> RunPermissionCardModel(
                title = "Keep live run status visible",
                message = "Notifications keep the foreground run visible while you switch apps, lock the phone, or want quick access back to the active session.",
                primaryLabel = "Allow notifications",
                secondaryLabel = "App settings",
                action = RunPermissionCardAction.REQUEST_NOTIFICATIONS,
            )

            context.shouldShowBackgroundPermissionCard &&
                (context.isTracking || context.hasPausedRun) &&
                !context.hasBackgroundPermission -> RunPermissionCardModel(
                title = "Track outside the app",
                message = if (context.shouldShowBackgroundRationale) {
                    "Background location keeps your route flowing when you switch apps. You can keep using foreground tracking if you prefer."
                } else if (context.shouldOpenBackgroundSettings) {
                    "Android 11 and newer manage all-the-time location from system settings. Open app settings if you want runs to continue more reliably after leaving the app."
                } else {
                    "Foreground tracking is ready. Add background access if you want the run to continue more reliably after leaving the app."
                },
                primaryLabel = if (context.shouldOpenBackgroundSettings) "Open settings" else "Background access",
                secondaryLabel = "App settings",
                action = if (context.shouldOpenBackgroundSettings) {
                    RunPermissionCardAction.OPEN_BACKGROUND_SETTINGS
                } else {
                    RunPermissionCardAction.REQUEST_BACKGROUND
                },
            )

            else -> null
        }

        return RunPermissionUiState(
            primaryActionLabel = primaryLabel,
            primaryAction = primaryAction,
            permissionCard = permissionCard,
            shouldRequestNotificationsBeforeTracking = context.shouldPromptNotificationPermission &&
                context.hasLocationPermission &&
                !context.hasNotificationPermission &&
                !context.isTracking,
        )
    }
}
