package com.example.productivityapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.viewmodel.SettingsUiState
import com.example.productivityapp.viewmodel.SettingsViewModel
import com.example.productivityapp.viewmodel.SettingsViewModelFactory

internal enum class SettingsToneKey {
    PROFILE,
    APPEARANCE,
    STEPS,
    RUN,
    WATER,
    SLEEP,
    PRIVACY,
}

internal fun settingsToneFor(key: SettingsToneKey, darkTheme: Boolean): SettingsSectionTone = when (key) {
    SettingsToneKey.PROFILE -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF271F31), Color(0xFF342942), Color(0xFFD0B2F1))
    } else {
        SettingsSectionTone(Color(0xFFEDE4F0), Color(0xFFF8EEFA), Color(0xFF8660A7))
    }

    SettingsToneKey.APPEARANCE -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF30211C), Color(0xFF422F29), Color(0xFFFFC4AA))
    } else {
        SettingsSectionTone(Color(0xFFF5E7E2), Color(0xFFFFF1EB), Color(0xFFC26C49))
    }

    SettingsToneKey.STEPS -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF312712), Color(0xFF45381A), Color(0xFFF0C867))
    } else {
        SettingsSectionTone(Color(0xFFF8EED3), Color(0xFFFFF6E3), Color(0xFFB98612))
    }

    SettingsToneKey.RUN -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF302018), Color(0xFF442C22), Color(0xFFFFB692))
    } else {
        SettingsSectionTone(Color(0xFFF6E4DB), Color(0xFFFFF0EA), Color(0xFFC56639))
    }

    SettingsToneKey.WATER -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF15263A), Color(0xFF1D3450), Color(0xFF91C8FF))
    } else {
        SettingsSectionTone(Color(0xFFE6F1FB), Color(0xFFF0F7FF), Color(0xFF2A6CC1))
    }

    SettingsToneKey.SLEEP -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF17251B), Color(0xFF223326), Color(0xFFA8E1B1))
    } else {
        SettingsSectionTone(Color(0xFFE6F2E8), Color(0xFFF1F8F2), Color(0xFF2F8F44))
    }

    SettingsToneKey.PRIVACY -> if (darkTheme) {
        SettingsSectionTone(Color(0xFF231E29), Color(0xFF2F2837), Color(0xFFC4B8D3))
    } else {
        SettingsSectionTone(Color(0xFFEEEAF1), Color(0xFFF6F3F8), Color(0xFF7A6A8A))
    }
}

@Composable
internal fun rememberSharedSettingsViewModel(): SettingsViewModel {
    val context = LocalContext.current
    return viewModel(
        factory = SettingsViewModelFactory(
            profileRepository = RepositoryProvider.provideUserProfileRepository(context),
            themeRepository = RepositoryProvider.provideAppThemeRepository(context),
        )
    )
}

@Composable
internal fun SettingsPopupScaffold(
    title: String,
    message: String?,
    isSaving: Boolean,
    canDismiss: Boolean,
    dismissLabel: String = "Cancel",
    saveLabel: String = "Save",
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (canDismiss) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    content()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (canDismiss) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(dismissLabel)
                        }
                    }
                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (isSaving) "Saving…" else saveLabel)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MandatoryProfileSetupDialog(
    uiState: SettingsUiState,
    onDisplayNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onGenderChanged: (String?) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    SettingsPopupScaffold(
        title = "Complete your profile",
        message = uiState.message ?: "Please add your name and age before using the app. Height, weight, and gender are optional.",
        isSaving = uiState.isSaving,
        canDismiss = false,
        saveLabel = "Save profile",
        onDismiss = {},
        onSave = onSave,
    ) {
        item {
            ProfileSettingsSection(
                state = uiState.profile,
                tone = settingsToneFor(SettingsToneKey.PROFILE, isDark),
                icon = Icons.Filled.PersonOutline,
                onOpenAvatarEditor = {},
                onDisplayNameChanged = onDisplayNameChanged,
                onAgeChanged = onAgeChanged,
                onGenderChanged = onGenderChanged,
                onHeightChanged = onHeightChanged,
                onWeightChanged = onWeightChanged,
            )
        }
    }
}

@Composable
internal fun StepsFeatureSettingsDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onDailyStepGoalChanged: (String) -> Unit,
    onStrideLengthChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    SettingsPopupScaffold(
        title = "Step settings",
        message = uiState.message,
        isSaving = uiState.isSaving,
        canDismiss = true,
        onDismiss = onDismiss,
        onSave = onSave,
    ) {
        item {
            StepsSettingsSection(
                state = uiState.steps,
                tone = settingsToneFor(SettingsToneKey.STEPS, isDark),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                onDailyStepGoalChanged = onDailyStepGoalChanged,
                onStrideLengthChanged = onStrideLengthChanged,
            )
        }
    }
}

@Composable
internal fun RunFeatureSettingsDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onPreferredUnitsChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    SettingsPopupScaffold(
        title = "Run settings",
        message = uiState.message,
        isSaving = uiState.isSaving,
        canDismiss = true,
        onDismiss = onDismiss,
        onSave = onSave,
    ) {
        item {
            RunSettingsSection(
                state = uiState.run,
                tone = settingsToneFor(SettingsToneKey.RUN, isDark),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                onPreferredUnitsChanged = onPreferredUnitsChanged,
            )
        }
    }
}

@Composable
internal fun WaterFeatureSettingsDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onDailyWaterGoalChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    SettingsPopupScaffold(
        title = "Water settings",
        message = uiState.message,
        isSaving = uiState.isSaving,
        canDismiss = true,
        onDismiss = onDismiss,
        onSave = onSave,
    ) {
        item {
            WaterSettingsSection(
                state = uiState.water,
                tone = settingsToneFor(SettingsToneKey.WATER, isDark),
                icon = Icons.Filled.WaterDrop,
                onDailyWaterGoalChanged = onDailyWaterGoalChanged,
            )
        }
    }
}

@Composable
internal fun SleepFeatureSettingsDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onNightlySleepGoalChanged: (String) -> Unit,
    onTypicalBedtimeChanged: (String) -> Unit,
    onTypicalWakeTimeChanged: (String) -> Unit,
    onSleepDetectionBufferChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    SettingsPopupScaffold(
        title = "Sleep settings",
        message = uiState.message,
        isSaving = uiState.isSaving,
        canDismiss = true,
        onDismiss = onDismiss,
        onSave = onSave,
    ) {
        item {
            SleepSettingsSection(
                state = uiState.sleep,
                tone = settingsToneFor(SettingsToneKey.SLEEP, isDark),
                icon = Icons.Filled.Hotel,
                onNightlySleepGoalChanged = onNightlySleepGoalChanged,
                onTypicalBedtimeChanged = onTypicalBedtimeChanged,
                onTypicalWakeTimeChanged = onTypicalWakeTimeChanged,
                onSleepDetectionBufferChanged = onSleepDetectionBufferChanged,
            )
        }
    }
}
