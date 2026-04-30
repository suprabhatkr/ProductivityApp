package com.example.productivityapp.ui.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPermissionCoordinatorTest {

    private val coordinator = RunPermissionCoordinator()

    @Test
    fun locationDenied_requestsLocationFirst() {
        val uiState = coordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = false,
                hasNotificationPermission = false,
                hasBackgroundPermission = false,
                shouldShowBackgroundRationale = false,
                shouldPromptNotificationPermission = true,
                shouldShowBackgroundPermissionCard = true,
                shouldOpenBackgroundSettings = false,
                isTracking = false,
                hasPausedRun = false,
            )
        )

        assertEquals(RunPrimaryAction.REQUEST_LOCATION, uiState.primaryAction)
        assertEquals("Enable Location", uiState.primaryActionLabel)
        assertEquals(RunPermissionCardAction.REQUEST_LOCATION, uiState.permissionCard?.action)
        assertFalse(uiState.shouldRequestNotificationsBeforeTracking)
    }

    @Test
    fun activeRun_prioritizesNotificationCardBeforeBackgroundCard() {
        val uiState = coordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = true,
                hasNotificationPermission = false,
                hasBackgroundPermission = false,
                shouldShowBackgroundRationale = true,
                shouldPromptNotificationPermission = true,
                shouldShowBackgroundPermissionCard = true,
                shouldOpenBackgroundSettings = false,
                isTracking = true,
                hasPausedRun = false,
            )
        )

        assertEquals(RunPrimaryAction.STOP_RUN, uiState.primaryAction)
        assertNotNull(uiState.permissionCard)
        assertEquals(RunPermissionCardAction.REQUEST_NOTIFICATIONS, uiState.permissionCard?.action)
        assertEquals("Allow notifications", uiState.permissionCard?.primaryLabel)
    }

    @Test
    fun pausedRun_showsBackgroundCardAfterLocationGranted() {
        val uiState = coordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = true,
                hasNotificationPermission = true,
                hasBackgroundPermission = false,
                shouldShowBackgroundRationale = false,
                shouldPromptNotificationPermission = true,
                shouldShowBackgroundPermissionCard = true,
                shouldOpenBackgroundSettings = false,
                isTracking = false,
                hasPausedRun = true,
            )
        )

        assertEquals(RunPrimaryAction.START_OR_RESUME_RUN, uiState.primaryAction)
        assertEquals(RunPermissionCardAction.REQUEST_BACKGROUND, uiState.permissionCard?.action)
        assertTrue(uiState.shouldRequestNotificationsBeforeTracking.not())
    }

    @Test
    fun readyToRun_requestsNotificationsWhenStartingButDoesNotShowCardYet() {
        val uiState = coordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = true,
                hasNotificationPermission = false,
                hasBackgroundPermission = false,
                shouldShowBackgroundRationale = false,
                shouldPromptNotificationPermission = true,
                shouldShowBackgroundPermissionCard = true,
                shouldOpenBackgroundSettings = false,
                isTracking = false,
                hasPausedRun = false,
            )
        )

        assertEquals(RunPrimaryAction.START_OR_RESUME_RUN, uiState.primaryAction)
        assertEquals("Start Run", uiState.primaryActionLabel)
        assertNull(uiState.permissionCard)
        assertTrue(uiState.shouldRequestNotificationsBeforeTracking)
    }

    @Test
    fun preAndroid13_notificationPermissionIsNotPrompted() {
        val uiState = coordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = true,
                hasNotificationPermission = true,
                hasBackgroundPermission = false,
                shouldShowBackgroundRationale = false,
                shouldPromptNotificationPermission = false,
                shouldShowBackgroundPermissionCard = true,
                shouldOpenBackgroundSettings = false,
                isTracking = false,
                hasPausedRun = false,
            )
        )

        assertEquals(RunPrimaryAction.START_OR_RESUME_RUN, uiState.primaryAction)
        assertFalse(uiState.shouldRequestNotificationsBeforeTracking)
    }

    @Test
    fun android11PausedRun_usesSettingsCtaForBackgroundLocation() {
        val uiState = coordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = true,
                hasNotificationPermission = true,
                hasBackgroundPermission = false,
                shouldShowBackgroundRationale = false,
                shouldPromptNotificationPermission = true,
                shouldShowBackgroundPermissionCard = true,
                shouldOpenBackgroundSettings = true,
                isTracking = false,
                hasPausedRun = true,
            )
        )

        assertEquals(RunPermissionCardAction.OPEN_BACKGROUND_SETTINGS, uiState.permissionCard?.action)
        assertEquals("Open settings", uiState.permissionCard?.primaryLabel)
    }
}
