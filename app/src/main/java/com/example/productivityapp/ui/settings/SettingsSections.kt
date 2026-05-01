package com.example.productivityapp.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.productivityapp.data.model.AppThemePreference
import com.example.productivityapp.ui.avatar.AvatarPreview
import com.example.productivityapp.viewmodel.AppearanceSettingsSectionState
import com.example.productivityapp.viewmodel.ProfileSettingsSectionState
import com.example.productivityapp.viewmodel.RunSettingsSectionState
import com.example.productivityapp.viewmodel.SleepSettingsSectionState
import com.example.productivityapp.viewmodel.StepSettingsSectionState
import com.example.productivityapp.viewmodel.SupportedGenderOptions
import com.example.productivityapp.viewmodel.WaterSettingsSectionState

data class SettingsSectionTone(
    val container: Color,
    val badge: Color,
    val accent: Color,
)

data class SettingsChoiceOption<T>(
    val value: T,
    val label: String,
)

@Composable
fun ProfileSettingsSection(
    state: ProfileSettingsSectionState,
    tone: SettingsSectionTone,
    icon: ImageVector,
    onOpenAvatarEditor: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onGenderChanged: (String?) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
) {
    SettingsSectionCard(
        title = "Profile",
        subtitle = "These values stay on your device and personalize your health features.",
        tone = tone,
        icon = icon,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Edit avatar"
                    role = Role.Button
                }
                .clickable(onClick = onOpenAvatarEditor),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarPreview(
                    avatar = state.avatar,
                    size = 84.dp,
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Profile avatar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Personalize your look with skin, hair, glasses, hats, and more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onOpenAvatarEditor) {
                    Text("Edit")
                }
            }
        }
        SettingsTextField(
            value = state.displayName,
            onValueChange = onDisplayNameChanged,
            label = "Name",
            tone = tone,
            errorText = state.displayNameError,
        )
        SettingsTextField(
            value = state.ageYears,
            onValueChange = onAgeChanged,
            label = "Age",
            tone = tone,
            keyboardType = KeyboardType.Number,
            errorText = state.ageYearsError,
        )
        SettingsChoiceRow(
            title = "Gender",
            options = listOf(SettingsChoiceOption<String?>(null, "Not set")) +
                SupportedGenderOptions.map { SettingsChoiceOption<String?>(it, it) },
            selected = state.gender,
            tone = tone,
            onSelected = onGenderChanged,
        )
        SettingsTextField(
            value = state.heightCm,
            onValueChange = onHeightChanged,
            label = "Height (cm)",
            tone = tone,
            keyboardType = KeyboardType.Number,
        )
        SettingsTextField(
            value = state.weightKg,
            onValueChange = onWeightChanged,
            label = "Weight (kg)",
            tone = tone,
            keyboardType = KeyboardType.Decimal,
        )
    }
}

@Composable
fun AppearanceSettingsSection(
    state: AppearanceSettingsSectionState,
    tone: SettingsSectionTone,
    icon: ImageVector,
    onThemePreferenceChanged: (AppThemePreference) -> Unit,
) {
    SettingsSectionCard(
        title = "Appearance",
        subtitle = "Choose the app theme for every screen. System stays in sync with your device.",
        tone = tone,
        icon = icon,
    ) {
        SettingsChoiceRow(
            title = "Theme",
            options = listOf(
                SettingsChoiceOption(AppThemePreference.SYSTEM, "System"),
                SettingsChoiceOption(AppThemePreference.LIGHT, "Light"),
                SettingsChoiceOption(AppThemePreference.DARK, "Dark"),
            ),
            selected = state.themePreference,
            tone = tone,
            onSelected = onThemePreferenceChanged,
        )
    }
}

@Composable
fun StepsSettingsSection(
    state: StepSettingsSectionState,
    tone: SettingsSectionTone,
    icon: ImageVector,
    onDailyStepGoalChanged: (String) -> Unit,
    onStrideLengthChanged: (String) -> Unit,
) {
    SettingsSectionCard(
        title = "Steps",
        subtitle = "Keep your daily target and stride estimate tuned for more useful progress cards.",
        tone = tone,
        icon = icon,
    ) {
        SettingsTextField(
            value = state.dailyStepGoal,
            onValueChange = onDailyStepGoalChanged,
            label = "Daily step goal",
            tone = tone,
            keyboardType = KeyboardType.Number,
        )
        SettingsTextField(
            value = state.strideLengthMeters,
            onValueChange = onStrideLengthChanged,
            label = "Stride length (meters)",
            tone = tone,
            keyboardType = KeyboardType.Decimal,
            supportingText = "Used by both step and run distance estimates.",
        )
    }
}

@Composable
fun RunSettingsSection(
    state: RunSettingsSectionState,
    tone: SettingsSectionTone,
    icon: ImageVector,
    onPreferredUnitsChanged: (String) -> Unit,
) {
    SettingsSectionCard(
        title = "Run",
        subtitle = "Pick how distance and pace are displayed when you open run details and history.",
        tone = tone,
        icon = icon,
    ) {
        SettingsChoiceRow(
            title = "Preferred units",
            options = listOf(
                SettingsChoiceOption("metric", "Metric"),
                SettingsChoiceOption("imperial", "Imperial"),
            ),
            selected = state.preferredUnits,
            tone = tone,
            onSelected = onPreferredUnitsChanged,
        )
    }
}

@Composable
fun WaterSettingsSection(
    state: WaterSettingsSectionState,
    tone: SettingsSectionTone,
    icon: ImageVector,
    onDailyWaterGoalChanged: (String) -> Unit,
) {
    SettingsSectionCard(
        title = "Water",
        subtitle = "Your daily goal powers the hydration ring, quick insights, and progress messaging.",
        tone = tone,
        icon = icon,
    ) {
        SettingsTextField(
            value = state.dailyWaterGoalMl,
            onValueChange = onDailyWaterGoalChanged,
            label = "Daily water goal (ml)",
            tone = tone,
            keyboardType = KeyboardType.Number,
        )
    }
}

@Composable
fun SleepSettingsSection(
    state: SleepSettingsSectionState,
    tone: SettingsSectionTone,
    icon: ImageVector,
    onNightlySleepGoalChanged: (String) -> Unit,
    onTypicalBedtimeChanged: (String) -> Unit,
    onTypicalWakeTimeChanged: (String) -> Unit,
    onSleepDetectionBufferChanged: (String) -> Unit,
) {
    SettingsSectionCard(
        title = "Sleep",
        subtitle = "These defaults help sleep summaries and on-device detection stay aligned with your routine.",
        tone = tone,
        icon = icon,
    ) {
        SettingsTextField(
            value = state.nightlySleepGoalMinutes,
            onValueChange = onNightlySleepGoalChanged,
            label = "Nightly sleep goal (minutes)",
            tone = tone,
            keyboardType = KeyboardType.Number,
            supportingText = "Typical range: 180 to 720 minutes.",
        )
        SettingsTextField(
            value = state.typicalBedtime,
            onValueChange = onTypicalBedtimeChanged,
            label = "Typical bedtime (HH:mm)",
            tone = tone,
            supportingText = "Example: 22:30",
        )
        SettingsTextField(
            value = state.typicalWakeTime,
            onValueChange = onTypicalWakeTimeChanged,
            label = "Typical wake time (HH:mm)",
            tone = tone,
            supportingText = "Example: 07:00",
        )
        SettingsTextField(
            value = state.sleepDetectionBufferMinutes,
            onValueChange = onSleepDetectionBufferChanged,
            label = "Sleep detection buffer (minutes)",
            tone = tone,
            keyboardType = KeyboardType.Number,
            supportingText = "Keeps noisy inactivity from shifting your sleep window too aggressively.",
        )
    }
}

@Composable
fun PrivacySettingsSection(
    tone: SettingsSectionTone,
    icon: ImageVector,
) {
    SettingsSectionCard(
        title = "Privacy & storage",
        subtitle = "Settings stay on this device and are used only to make your summaries, estimates, and reminders feel more personal.",
        tone = tone,
        icon = icon,
    ) {
        Text(
            text = "Profile values are stored locally. Reset returns optional profile fields to empty values and restores feature defaults.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    tone: SettingsSectionTone,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tone.container),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = tone.badge,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tone.accent,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tone: SettingsSectionTone,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    errorText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = {
            when {
                errorText != null -> Text(errorText, color = MaterialTheme.colorScheme.error)
                supportingText != null -> Text(supportingText)
            }
        },
        isError = errorText != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tone.accent,
            focusedLabelColor = tone.accent,
            cursorColor = tone.accent,
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorCursorColor = MaterialTheme.colorScheme.error,
        ),
    )
}

@Composable
private fun <T> SettingsChoiceRow(
    title: String,
    options: List<SettingsChoiceOption<T>>,
    selected: T,
    tone: SettingsSectionTone,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                SettingsChoiceChip(
                    label = option.label,
                    selected = option.value == selected,
                    tone = tone,
                    onClick = { onSelected(option.value) },
                )
            }
        }
    }
}

@Composable
private fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    tone: SettingsSectionTone,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) tone.badge else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) tone.accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) tone.accent else MaterialTheme.colorScheme.onSurface,
        )
    }
}
