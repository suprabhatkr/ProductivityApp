package com.example.productivityapp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.WorkoutEntity
import com.example.productivityapp.data.entities.type
import com.example.productivityapp.data.model.WorkoutType
import com.example.productivityapp.viewmodel.WorkoutViewModel
import com.example.productivityapp.viewmodel.WorkoutViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val WorkoutBackdropLight = Color(0xFFF6F4FF)
private val WorkoutBackdropDark = Color(0xFF0F1220)
private val WorkoutSurfaceLight = Color(0xFFFFFFFF)
private val WorkoutSurfaceDark = Color(0xFF1A2033)
private val WorkoutSurfaceAltLight = Color(0xFFEEF2FF)
private val WorkoutSurfaceAltDark = Color(0xFF222A40)
private val WorkoutAccentLight = Color(0xFF4F46E5)
private val WorkoutAccentDark = Color(0xFFA5B4FC)
private val WorkoutToneLight = Color(0xFF3730A3)
private val WorkoutToneDark = Color(0xFFDBE4FF)
private val WorkoutChipLight = Color(0xFFE0E7FF)
private val WorkoutChipDark = Color(0xFF2A3350)
private val WorkoutSuccessLight = Color(0xFF0F9D58)
private val WorkoutSuccessDark = Color(0xFF7DE3A9)

private val WorkoutDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a")

@Composable
fun WorkoutScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = RepositoryProvider.provideWorkoutRepository(context)
    val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModelFactory(repository))
    val workouts by viewModel.workouts.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val selectedWorkoutType by viewModel.selectedWorkoutType.collectAsState()

    WorkoutScreenContent(
        workouts = workouts,
        activeWorkout = activeWorkout,
        selectedWorkoutType = selectedWorkoutType,
        onSelectWorkoutType = viewModel::selectWorkoutType,
        onStartWorkout = viewModel::startWorkout,
        onEndWorkout = viewModel::endWorkout,
        onBack = onBack,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun WorkoutScreenContent(
    workouts: List<WorkoutEntity>,
    activeWorkout: WorkoutEntity?,
    selectedWorkoutType: WorkoutType,
    onSelectWorkoutType: (WorkoutType) -> Unit,
    onStartWorkout: () -> Unit,
    onEndWorkout: () -> Unit,
    onBack: () -> Unit,
) {
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backdrop = if (darkTheme) WorkoutBackdropDark else WorkoutBackdropLight
    val surface = if (darkTheme) WorkoutSurfaceDark else WorkoutSurfaceLight
    val surfaceAlt = if (darkTheme) WorkoutSurfaceAltDark else WorkoutSurfaceAltLight
    val accent = if (darkTheme) WorkoutAccentDark else WorkoutAccentLight
    val tone = if (darkTheme) WorkoutToneDark else WorkoutToneLight
    val chip = if (darkTheme) WorkoutChipDark else WorkoutChipLight
    val success = if (darkTheme) WorkoutSuccessDark else WorkoutSuccessLight
    val currentTimeMs by produceState(initialValue = System.currentTimeMillis(), key1 = activeWorkout?.id) {
        value = System.currentTimeMillis()
        if (activeWorkout != null) {
            while (true) {
                delay(1_000L)
                value = System.currentTimeMillis()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = backdrop,
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = backdrop,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    WorkoutHeroCard(
                        activeWorkout = activeWorkout,
                        selectedWorkoutType = selectedWorkoutType,
                        surface = surface,
                        accent = accent,
                        chipColor = chip,
                        tone = tone,
                    )
                }

                item {
                    WorkoutActionCard(
                        activeWorkout = activeWorkout,
                        selectedWorkoutType = selectedWorkoutType,
                        currentTimeMs = currentTimeMs,
                        surface = surfaceAlt,
                        accent = accent,
                        success = success,
                        onStartWorkout = onStartWorkout,
                        onEndWorkout = onEndWorkout,
                    )
                }

                item {
                    Text(
                        "Choose your workout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        WorkoutType.entries.forEach { type ->
                            WorkoutTypeCard(
                                type = type,
                                isSelected = selectedWorkoutType == type,
                                enabled = activeWorkout == null,
                                surface = surface,
                                accent = accent,
                                tone = tone,
                                onClick = { onSelectWorkoutType(type) },
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Recent sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (workouts.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surface),
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Text(
                                "Start your first workout to build a history across indoor, outdoor, yoga, pushups, and more.",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(workouts.take(8), key = { it.id }) { workout ->
                        WorkoutHistoryCard(
                            workout = workout,
                            currentTimeMs = currentTimeMs,
                            surface = surface,
                            accent = accent,
                            success = success,
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkoutHeroCard(
    activeWorkout: WorkoutEntity?,
    selectedWorkoutType: WorkoutType,
    surface: Color,
    accent: Color,
    chipColor: Color,
    tone: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(top = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (activeWorkout == null) "Workout ready" else "${activeWorkout.type.label} in progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (activeWorkout == null) {
                            "Pick a workout style and begin when you are ready."
                        } else {
                            "Keep focus on your current session, then end it when you're done."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(color = chipColor, shape = RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForWorkoutType(activeWorkout?.type ?: selectedWorkoutType),
                        contentDescription = null,
                        tint = accent,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkoutChip(
                    text = "${workoutsLabel(activeWorkout, selectedWorkoutType)} selected",
                    background = chipColor,
                    color = tone,
                )
                WorkoutChip(
                    text = if (activeWorkout == null) "Start & end control" else "One active session",
                    background = chipColor,
                    color = tone,
                )
            }
        }
    }
}

@Composable
private fun WorkoutActionCard(
    activeWorkout: WorkoutEntity?,
    selectedWorkoutType: WorkoutType,
    currentTimeMs: Long,
    surface: Color,
    accent: Color,
    success: Color,
    onStartWorkout: () -> Unit,
    onEndWorkout: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Session controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (activeWorkout == null) {
                Text(
                    "Selected: ${selectedWorkoutType.label}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent,
                )
                Text(
                    "Track your workout duration with a single start button. End the session when you finish.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Start ${selectedWorkoutType.label}" },
                ) {
                    Text("Start ${selectedWorkoutType.label}")
                }
            } else {
                val elapsedSec = ((currentTimeMs - activeWorkout.startTime) / 1000L).coerceAtLeast(0L)
                Text(
                    activeWorkout.type.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = success,
                )
                Text(
                    "Started ${formatWorkoutDateTime(activeWorkout.startTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatWorkoutDuration(elapsedSec),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Active now")
                    }
                    Button(
                        onClick = onEndWorkout,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "End workout" },
                    ) {
                        Text("End workout")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTypeCard(
    type: WorkoutType,
    isSelected: Boolean,
    enabled: Boolean,
    surface: Color,
    accent: Color,
    tone: Color,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) accent.copy(alpha = 0.14f) else surface
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .width(160.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isSelected) accent.copy(alpha = 0.18f) else tone.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForWorkoutType(type),
                    contentDescription = null,
                    tint = if (isSelected) accent else tone,
                )
            }
            Text(
                type.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                when (type) {
                    WorkoutType.INDOOR -> "Home and gym-ready sessions."
                    WorkoutType.OUTDOOR -> "Fresh-air workouts on the move."
                    WorkoutType.INTENSE -> "High-energy, sweat-heavy focus."
                    WorkoutType.YOGA -> "Stretch, mobility, and breath."
                    WorkoutType.LIGHT -> "Gentle movement and recovery."
                    WorkoutType.PUSHUPS -> "Bodyweight upper-body session."
                    WorkoutType.OTHER -> "Anything else you want to track."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutHistoryCard(
    workout: WorkoutEntity,
    currentTimeMs: Long,
    surface: Color,
    accent: Color,
    success: Color,
) {
    val durationSec = workout.endTime?.let { workout.durationSec }
        ?: ((currentTimeMs - workout.startTime) / 1000L).coerceAtLeast(0L)
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForWorkoutType(workout.type),
                        contentDescription = null,
                        tint = accent,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        workout.type.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        formatWorkoutDateTime(workout.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatWorkoutDuration(durationSec),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (workout.endTime == null) "Live" else "Completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (workout.endTime == null) success else accent,
                )
            }
        }
    }
}

@Composable
private fun WorkoutChip(
    text: String,
    background: Color,
    color: Color,
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

private fun iconForWorkoutType(type: WorkoutType): ImageVector {
    return when (type) {
        WorkoutType.INDOOR -> Icons.Filled.Home
        WorkoutType.OUTDOOR -> Icons.Filled.Landscape
        WorkoutType.INTENSE -> Icons.Filled.Bolt
        WorkoutType.YOGA -> Icons.Filled.SelfImprovement
        WorkoutType.LIGHT -> Icons.Filled.Spa
        WorkoutType.PUSHUPS -> Icons.Filled.FitnessCenter
        WorkoutType.OTHER -> Icons.Filled.MoreHoriz
    }
}

private fun formatWorkoutDateTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(WorkoutDateFormatter)
}

internal fun formatWorkoutDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%dh %02dm".format(hours, minutes)
    } else if (minutes > 0L) {
        "%dm %02ds".format(minutes, seconds)
    } else {
        "%ds".format(seconds)
    }
}

private fun workoutsLabel(activeWorkout: WorkoutEntity?, selectedWorkoutType: WorkoutType): String {
    return activeWorkout?.type?.label ?: selectedWorkoutType.label
}
