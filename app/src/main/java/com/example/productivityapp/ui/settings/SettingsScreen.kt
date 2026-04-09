package com.example.productivityapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.productivityapp.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading your on-device profile…",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    uiState.message?.let { message ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Profile", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "These values stay on your device and are used for health feature personalization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = viewModel::updateDisplayName,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Display name") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.weightKg,
                                onValueChange = viewModel::updateWeightKg,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Weight (kg)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.heightCm,
                                onValueChange = viewModel::updateHeightCm,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Height (cm)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.strideLengthMeters,
                                onValueChange = viewModel::updateStrideLengthMeters,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Stride length (meters)") },
                                supportingText = { Text("Used by step and run distance estimates.") },
                                singleLine = true,
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Units & daily goals", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.updatePreferredUnits("metric") },
                                    enabled = uiState.preferredUnits != "metric",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Metric")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.updatePreferredUnits("imperial") },
                                    enabled = uiState.preferredUnits != "imperial",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Imperial")
                                }
                            }
                            OutlinedTextField(
                                value = uiState.dailyStepGoal,
                                onValueChange = viewModel::updateDailyStepGoal,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Daily step goal") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.dailyWaterGoalMl,
                                onValueChange = viewModel::updateDailyWaterGoalMl,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Daily water goal (ml)") },
                                singleLine = true,
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Privacy & storage", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Sensitive profile values are stored with encrypted shared preferences. Day-by-day counters and preferences are kept in local app storage on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Use Reset profile if you want to clear your saved name, height, weight, and restore default goals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = viewModel::resetProfile,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Reset profile")
                        }
                        Button(
                            onClick = viewModel::saveProfile,
                            enabled = uiState.hasUnsavedChanges && !uiState.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (uiState.isSaving) "Saving…" else "Save settings")
                        }
                    }
                }
            }
        }
    }
}

