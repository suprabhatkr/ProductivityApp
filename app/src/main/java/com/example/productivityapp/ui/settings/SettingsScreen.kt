package com.example.productivityapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarGlassesStyle
import com.example.productivityapp.data.model.AvatarHairStyle
import com.example.productivityapp.data.model.AvatarHatStyle
import com.example.productivityapp.data.model.AvatarPresentation
import com.example.productivityapp.data.model.AvatarSkinTone
import com.example.productivityapp.ui.avatar.AvatarPreview
import com.example.productivityapp.ui.avatar.AvatarSkinToneOptionCard
import com.example.productivityapp.ui.avatar.AvatarTraitOptionCard
import com.example.productivityapp.viewmodel.SettingsUiState
import com.example.productivityapp.viewmodel.SettingsViewModel

private val SettingsBackdropLight = Color(0xFFF8F4F7)
private val SettingsBackdropDark = Color(0xFF110E13)
private val SettingsSurfaceLight = Color(0xFFF0EAF1)
private val SettingsSurfaceDark = Color(0xFF1B1620)
private val SettingsSurfaceAltLight = Color(0xFFFFFFFF)
private val SettingsSurfaceAltDark = Color(0xFF221C29)

private val ProfileToneLight = SettingsSectionTone(Color(0xFFEDE4F0), Color(0xFFF8EEFA), Color(0xFF8660A7))
private val ProfileToneDark = SettingsSectionTone(Color(0xFF271F31), Color(0xFF342942), Color(0xFFD0B2F1))
private val AppearanceToneLight = SettingsSectionTone(Color(0xFFF5E7E2), Color(0xFFFFF1EB), Color(0xFFC26C49))
private val AppearanceToneDark = SettingsSectionTone(Color(0xFF30211C), Color(0xFF422F29), Color(0xFFFFC4AA))
private val StepsToneLight = SettingsSectionTone(Color(0xFFF8EED3), Color(0xFFFFF6E3), Color(0xFFB98612))
private val StepsToneDark = SettingsSectionTone(Color(0xFF312712), Color(0xFF45381A), Color(0xFFF0C867))
private val RunToneLight = SettingsSectionTone(Color(0xFFF6E4DB), Color(0xFFFFF0EA), Color(0xFFC56639))
private val RunToneDark = SettingsSectionTone(Color(0xFF302018), Color(0xFF442C22), Color(0xFFFFB692))
private val WaterToneLight = SettingsSectionTone(Color(0xFFE6F1FB), Color(0xFFF0F7FF), Color(0xFF2A6CC1))
private val WaterToneDark = SettingsSectionTone(Color(0xFF15263A), Color(0xFF1D3450), Color(0xFF91C8FF))
private val SleepToneLight = SettingsSectionTone(Color(0xFFE6F2E8), Color(0xFFF1F8F2), Color(0xFF2F8F44))
private val SleepToneDark = SettingsSectionTone(Color(0xFF17251B), Color(0xFF223326), Color(0xFFA8E1B1))
private val PrivacyToneLight = SettingsSectionTone(Color(0xFFEEEAF1), Color(0xFFF6F3F8), Color(0xFF7A6A8A))
private val PrivacyToneDark = SettingsSectionTone(Color(0xFF231E29), Color(0xFF2F2837), Color(0xFFC4B8D3))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onDisplayNameChanged = viewModel::updateDisplayName,
        onAgeChanged = viewModel::updateAgeYears,
        onGenderChanged = viewModel::updateGender,
        onHeightChanged = viewModel::updateHeightCm,
        onWeightChanged = viewModel::updateWeightKg,
        onDailyStepGoalChanged = viewModel::updateDailyStepGoal,
        onStrideLengthChanged = viewModel::updateStrideLengthMeters,
        onPreferredUnitsChanged = viewModel::updatePreferredUnits,
        onDailyWaterGoalChanged = viewModel::updateDailyWaterGoalMl,
        onNightlySleepGoalChanged = viewModel::updateNightlySleepGoalMinutes,
        onTypicalBedtimeChanged = viewModel::updateTypicalBedtime,
        onTypicalWakeTimeChanged = viewModel::updateTypicalWakeTime,
        onSleepDetectionBufferChanged = viewModel::updateSleepDetectionBufferMinutes,
        onOpenAvatarEditor = viewModel::openAvatarEditor,
        onDismissAvatarEditor = viewModel::dismissAvatarEditor,
        onApplyAvatarDraft = viewModel::applyAvatarDraft,
        onResetAvatarDraftToSaved = viewModel::resetAvatarDraftToSaved,
        onAvatarSkinToneChanged = viewModel::updateAvatarSkinTone,
        onAvatarPresentationChanged = viewModel::updateAvatarPresentation,
        onAvatarHairStyleChanged = viewModel::updateAvatarHairStyle,
        onAvatarGlassesStyleChanged = viewModel::updateAvatarGlassesStyle,
        onAvatarHatStyleChanged = viewModel::updateAvatarHatStyle,
        onReset = viewModel::resetSettings,
        onSave = { viewModel.saveSettings() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onGenderChanged: (String?) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onDailyStepGoalChanged: (String) -> Unit,
    onStrideLengthChanged: (String) -> Unit,
    onPreferredUnitsChanged: (String) -> Unit,
    onDailyWaterGoalChanged: (String) -> Unit,
    onNightlySleepGoalChanged: (String) -> Unit,
    onTypicalBedtimeChanged: (String) -> Unit,
    onTypicalWakeTimeChanged: (String) -> Unit,
    onSleepDetectionBufferChanged: (String) -> Unit,
    onOpenAvatarEditor: () -> Unit,
    onDismissAvatarEditor: () -> Unit,
    onApplyAvatarDraft: () -> Unit,
    onResetAvatarDraftToSaved: () -> Unit,
    onAvatarSkinToneChanged: (AvatarSkinTone) -> Unit,
    onAvatarPresentationChanged: (AvatarPresentation) -> Unit,
    onAvatarHairStyleChanged: (AvatarHairStyle) -> Unit,
    onAvatarGlassesStyleChanged: (AvatarGlassesStyle) -> Unit,
    onAvatarHatStyleChanged: (AvatarHatStyle) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val backdrop = if (isDark) SettingsBackdropDark else SettingsBackdropLight
    val surface = if (isDark) SettingsSurfaceDark else SettingsSurfaceLight
    val surfaceAlt = if (isDark) SettingsSurfaceAltDark else SettingsSurfaceAltLight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                expandedHeight = 56.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = backdrop,
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backdrop),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = surfaceAlt,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Text(
                            text = "Loading your settings…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backdrop),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SettingsHeroCard(
                        uiState = uiState,
                        surface = surfaceAlt,
                    )
                }
                uiState.message?.let { message ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceAlt),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                item {
                    ProfileSettingsSection(
                        state = uiState.profile,
                        tone = if (isDark) ProfileToneDark else ProfileToneLight,
                        icon = Icons.Filled.PersonOutline,
                        onOpenAvatarEditor = onOpenAvatarEditor,
                        onDisplayNameChanged = onDisplayNameChanged,
                        onAgeChanged = onAgeChanged,
                        onGenderChanged = onGenderChanged,
                        onHeightChanged = onHeightChanged,
                        onWeightChanged = onWeightChanged,
                    )
                }
                item {
                    StepsSettingsSection(
                        state = uiState.steps,
                        tone = if (isDark) StepsToneDark else StepsToneLight,
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        onDailyStepGoalChanged = onDailyStepGoalChanged,
                        onStrideLengthChanged = onStrideLengthChanged,
                    )
                }
                item {
                    RunSettingsSection(
                        state = uiState.run,
                        tone = if (isDark) RunToneDark else RunToneLight,
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        onPreferredUnitsChanged = onPreferredUnitsChanged,
                    )
                }
                item {
                    WaterSettingsSection(
                        state = uiState.water,
                        tone = if (isDark) WaterToneDark else WaterToneLight,
                        icon = Icons.Filled.WaterDrop,
                        onDailyWaterGoalChanged = onDailyWaterGoalChanged,
                    )
                }
                item {
                    SleepSettingsSection(
                        state = uiState.sleep,
                        tone = if (isDark) SleepToneDark else SleepToneLight,
                        icon = Icons.Filled.Hotel,
                        onNightlySleepGoalChanged = onNightlySleepGoalChanged,
                        onTypicalBedtimeChanged = onTypicalBedtimeChanged,
                        onTypicalWakeTimeChanged = onTypicalWakeTimeChanged,
                        onSleepDetectionBufferChanged = onSleepDetectionBufferChanged,
                    )
                }
                item {
                    PrivacySettingsSection(
                        tone = if (isDark) PrivacyToneDark else PrivacyToneLight,
                        icon = Icons.Filled.Security,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Reset")
                        }
                        Button(
                            onClick = onSave,
                            enabled = uiState.hasUnsavedChanges && !uiState.isSaving,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text(if (uiState.isSaving) "Saving…" else "Save")
                        }
                    }
                }
            }
        }

        if (uiState.avatarEditor.isVisible) {
            AvatarEditorDialog(
                draft = uiState.avatarEditor.draft,
                onDismiss = onDismissAvatarEditor,
                onApply = onApplyAvatarDraft,
                onReset = onResetAvatarDraftToSaved,
                onSkinToneChanged = onAvatarSkinToneChanged,
                onPresentationChanged = onAvatarPresentationChanged,
                onHairStyleChanged = onAvatarHairStyleChanged,
                onGlassesStyleChanged = onAvatarGlassesStyleChanged,
                onHatStyleChanged = onAvatarHatStyleChanged,
            )
        }
    }
}

private enum class AvatarEditorTab(val label: String) {
    SKIN("Skin"),
    LOOK("Look"),
    HAIR("Hair"),
    GLASSES("Glasses"),
    HATS("Hats"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarEditorDialog(
    draft: AvatarConfig,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onSkinToneChanged: (AvatarSkinTone) -> Unit,
    onPresentationChanged: (AvatarPresentation) -> Unit,
    onHairStyleChanged: (AvatarHairStyle) -> Unit,
    onGlassesStyleChanged: (AvatarGlassesStyle) -> Unit,
    onHatStyleChanged: (AvatarHatStyle) -> Unit,
) {
    val selectedTab = remember { mutableStateOf(AvatarEditorTab.SKIN) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Edit avatar", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.semantics { contentDescription = "Close avatar editor" },
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    onApply()
                                },
                                modifier = Modifier.semantics { contentDescription = "Apply avatar changes" },
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AvatarPreview(
                                avatar = draft,
                                size = 156.dp,
                                contentDescription = "Avatar editor live preview",
                            )
                            Text(
                                text = "Mix skin tone, look, hair, glasses, and hats with a live preview.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = onReset,
                                modifier = Modifier.semantics { contentDescription = "Reset avatar draft" },
                            ) {
                                Text("Reset draft")
                            }
                        }
                    }
                    SecondaryScrollableTabRow(
                        selectedTabIndex = selectedTab.value.ordinal,
                    ) {
                        AvatarEditorTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab.value.ordinal == index,
                                onClick = { selectedTab.value = tab },
                                text = { Text(tab.label) },
                            )
                        }
                    }
                    when (selectedTab.value) {
                        AvatarEditorTab.SKIN -> AvatarGrid {
                            items(AvatarSkinTone.entries.size) { index ->
                                val option = AvatarSkinTone.entries[index]
                                AvatarSkinToneOptionCard(
                                    skinTone = option,
                                    selected = draft.skinTone == option,
                                    onClick = { onSkinToneChanged(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        AvatarEditorTab.LOOK -> AvatarGrid {
                            items(AvatarPresentation.entries.size) { index ->
                                val option = AvatarPresentation.entries[index]
                                AvatarTraitOptionCard(
                                    label = option.label,
                                    selected = draft.presentation == option,
                                    onClick = { onPresentationChanged(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    AvatarPreview(
                                        avatar = draft.copy(presentation = option),
                                        size = 56.dp,
                                        contentDescription = null,
                                    )
                                }
                            }
                        }

                        AvatarEditorTab.HAIR -> AvatarGrid {
                            items(AvatarHairStyle.entries.size) { index ->
                                val option = AvatarHairStyle.entries[index]
                                AvatarTraitOptionCard(
                                    label = option.label,
                                    selected = draft.hairStyle == option,
                                    onClick = { onHairStyleChanged(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    AvatarPreview(
                                        avatar = draft.copy(hairStyle = option),
                                        size = 56.dp,
                                        contentDescription = null,
                                    )
                                }
                            }
                        }

                        AvatarEditorTab.GLASSES -> AvatarGrid {
                            items(AvatarGlassesStyle.entries.size) { index ->
                                val option = AvatarGlassesStyle.entries[index]
                                AvatarTraitOptionCard(
                                    label = option.label,
                                    selected = draft.glassesStyle == option,
                                    onClick = { onGlassesStyleChanged(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    AvatarPreview(
                                        avatar = draft.copy(glassesStyle = option),
                                        size = 56.dp,
                                        contentDescription = null,
                                    )
                                }
                            }
                        }

                        AvatarEditorTab.HATS -> AvatarGrid {
                            items(AvatarHatStyle.entries.size) { index ->
                                val option = AvatarHatStyle.entries[index]
                                AvatarTraitOptionCard(
                                    label = option.label,
                                    selected = draft.hatStyle == option,
                                    onClick = { onHatStyleChanged(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    AvatarPreview(
                                        avatar = draft.copy(hatStyle = option),
                                        size = 56.dp,
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onApply,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Apply avatar changes button" },
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AvatarGrid(
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun SettingsHeroCard(
    uiState: SettingsUiState,
    surface: Color,
) {
    val filledProfileFields = listOf(
        uiState.profile.displayName.isNotBlank(),
        uiState.profile.ageYears.isNotBlank(),
        !uiState.profile.gender.isNullOrBlank(),
        uiState.profile.heightCm.isNotBlank(),
        uiState.profile.weightKg.isNotBlank(),
    ).count { it }
    val unitsLabel = if (uiState.run.preferredUnits == "imperial") "Imperial units" else "Metric units"

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Keep your profile and goals aligned across the app.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Everything here stays local to your device and is reused by the home, run, sleep, step, and water experiences.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsHeroChip(unitsLabel)
                SettingsHeroChip("Profile $filledProfileFields/5")
            }
        }
    }
}

@Composable
private fun SettingsHeroChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
