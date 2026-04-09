package com.example.productivityapp.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.viewmodel.SleepDaySummary
import com.example.productivityapp.viewmodel.SleepViewModel
import com.example.productivityapp.viewmodel.SleepViewModelFactory
import kotlin.math.roundToInt

@Composable
fun SleepScreen() {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideSleepRepository(context)
    val vm: SleepViewModel = viewModel(factory = SleepViewModelFactory(repo))

    val sessions = vm.sessions.collectAsState()
    val weeklySummary = vm.weeklySummary.collectAsState()
    val activeSession = vm.activeSession.collectAsState()
    val elapsedSeconds = vm.elapsedSeconds.collectAsState()
    val isPaused = vm.isPaused.collectAsState()
    val pendingReview = vm.pendingReviewSession.collectAsState()

    SleepScreenContent(
        sessions = sessions.value,
        weeklySummary = weeklySummary.value,
        activeSession = activeSession.value,
        elapsedSeconds = elapsedSeconds.value,
        isPaused = isPaused.value,
        pendingReviewSession = pendingReview.value,
        onStartSleep = vm::startSleep,
        onPauseSleep = vm::pauseSleep,
        onResumeSleep = vm::resumeSleep,
        onStopSleep = vm::stopSleep,
        onSubmitReview = vm::submitSleepReview,
        onDismissReview = vm::dismissSleepReview,
    )
}

@Composable
fun SleepScreenContent(
    sessions: List<SleepEntity>,
    weeklySummary: List<SleepDaySummary>,
    activeSession: SleepEntity?,
    elapsedSeconds: Long,
    isPaused: Boolean,
    pendingReviewSession: SleepEntity?,
    onStartSleep: () -> Unit,
    onPauseSleep: () -> Unit,
    onResumeSleep: () -> Unit,
    onStopSleep: () -> Unit,
    onSubmitReview: (Int, String) -> Unit,
    onDismissReview: () -> Unit,
) {
    var quality by remember(pendingReviewSession?.id) { mutableIntStateOf(4) }
    var notes by remember(pendingReviewSession?.id) { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sleep Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Goal: 8 hours", style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                ActiveSleepCard(
                    activeSession = activeSession,
                    elapsedSeconds = elapsedSeconds,
                    isPaused = isPaused,
                    onStartSleep = onStartSleep,
                    onPauseSleep = onPauseSleep,
                    onResumeSleep = onResumeSleep,
                    onStopSleep = onStopSleep,
                )
            }

            item {
                WeeklySleepChart(weeklySummary = weeklySummary)
            }

            if (pendingReviewSession != null) {
                item {
                    SleepReviewCard(
                        quality = quality,
                        notes = notes,
                        onQualitySelected = { quality = it },
                        onNotesChanged = { notes = it },
                        onSave = { onSubmitReview(quality, notes) },
                        onDismiss = onDismissReview,
                    )
                }
            }

            item {
                Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            items(sessions) { session ->
                SleepHistoryCard(session)
            }
        }
    }
}

@Composable
private fun ActiveSleepCard(
    activeSession: SleepEntity?,
    elapsedSeconds: Long,
    isPaused: Boolean,
    onStartSleep: () -> Unit,
    onPauseSleep: () -> Unit,
    onResumeSleep: () -> Unit,
    onStopSleep: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (activeSession == null) {
                Text("No active sleep session")
                Button(onClick = onStartSleep) {
                    Text("Start Sleep")
                }
            } else {
                Text("Started: ${formatTimestamp(activeSession.startTimestamp)}")
                Text("Elapsed: ${formatDuration(elapsedSeconds)}", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isPaused) {
                        OutlinedButton(onClick = onResumeSleep) { Text("Resume") }
                    } else {
                        OutlinedButton(onClick = onPauseSleep) { Text("Pause") }
                    }
                    Button(onClick = onStopSleep) { Text("Stop") }
                }
            }
        }
    }
}

@Composable
private fun WeeklySleepChart(weeklySummary: List<SleepDaySummary>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Last 7 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val maxDuration = (weeklySummary.maxOfOrNull { it.totalDurationSec } ?: 1L).coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                weeklySummary.forEach { day ->
                    val ratio = (day.totalDurationSec.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatHours(day.totalDurationSec), style = MaterialTheme.typography.labelSmall)
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((120 * ratio).coerceAtLeast(8f).dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                        Text(day.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepReviewCard(
    quality: Int,
    notes: String,
    onQualitySelected: (Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How did you sleep?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            (1..5).forEach { rating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = quality == rating, onClick = { onQualitySelected(rating) })
                    Text("$rating / 5")
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("Save review") }
                OutlinedButton(onClick = onDismiss) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun SleepHistoryCard(session: SleepEntity) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(session.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text("${formatTimestamp(session.startTimestamp)} → ${formatTimestamp(session.endTimestamp)}")
            Text("Duration: ${formatDuration(session.durationSec)}")
            Text("Quality: ${session.sleepQuality ?: "--"}/5")
            if (!session.notes.isNullOrBlank()) {
                Text("Notes: ${session.notes}")
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "%dh %02dm".format(hours, minutes) else "%dm".format(minutes)
}

private fun formatHours(totalSeconds: Long): String {
    return "%.1fh".format(totalSeconds / 3600.0)
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "--"
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
    return "%02d:%02d".format(zoned.hour, zoned.minute)
}

