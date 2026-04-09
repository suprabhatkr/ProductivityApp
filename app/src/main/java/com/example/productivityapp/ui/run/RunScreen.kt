package com.example.productivityapp.ui.run

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.run.RunReplayHelper
import com.example.productivityapp.service.RunTrackingService
import com.example.productivityapp.viewmodel.RunViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunScreen() {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideRunRepository(context)
    val vm: com.example.productivityapp.viewmodel.RunViewModel = viewModel(factory = RunViewModelFactory(repo))
    val runs = vm.runs.collectAsState()
    val latest = runs.value.firstOrNull()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val backgroundPermissionState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    var running by remember { mutableStateOf(false) }
    var replayIndex by remember(latest?.id) { mutableIntStateOf(0) }
    var replayPlaying by remember(latest?.id) { mutableStateOf(false) }

    val replayPoints = remember(latest?.polyline) {
        RunReplayHelper.decodeEncodedPolyline(latest?.polyline.orEmpty())
    }

    LaunchedEffect(replayPlaying, replayPoints.size) {
        if (!replayPlaying) return@LaunchedEffect
        while (replayPlaying && replayPoints.isNotEmpty()) {
            delay(600)
            if (replayIndex >= replayPoints.lastIndex) {
                replayPlaying = false
            } else {
                replayIndex += 1
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("Run Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Past runs: ${runs.value.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                when (locationPermissionState.status) {
                    is PermissionStatus.Granted -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = {
                                        val intent = Intent(context, RunTrackingService::class.java)
                                        if (!running) {
                                            intent.action = RunTrackingService.ACTION_START
                                            context.startForegroundService(intent)
                                            running = true
                                        } else {
                                            intent.action = RunTrackingService.ACTION_STOP
                                            context.startService(intent)
                                            running = false
                                        }
                                    }) {
                                        Text(if (!running) "Start Run" else "Stop Run")
                                    }

                                    if (running) {
                                        OutlinedButton(onClick = {
                                            val intent = Intent(context, RunTrackingService::class.java).apply {
                                                action = RunTrackingService.ACTION_PAUSE
                                            }
                                            context.startService(intent)
                                            running = false
                                        }) {
                                            Text("Pause")
                                        }
                                    } else if (latest?.endTime == null && latest != null) {
                                        OutlinedButton(onClick = {
                                            val intent = Intent(context, RunTrackingService::class.java).apply {
                                                action = RunTrackingService.ACTION_RESUME
                                            }
                                            context.startService(intent)
                                            running = true
                                        }) {
                                            Text("Resume")
                                        }
                                    }
                                }

                                when (val bg = backgroundPermissionState.status) {
                                    is PermissionStatus.Granted -> {
                                        Text("Background location enabled", style = MaterialTheme.typography.bodySmall)
                                    }
                                    is PermissionStatus.Denied -> {
                                        if (bg.shouldShowRationale) {
                                            AlertDialog(
                                                onDismissRequest = {},
                                                confirmButton = {
                                                    TextButton(onClick = { backgroundPermissionState.launchPermissionRequest() }) {
                                                        Text("Allow Background")
                                                    }
                                                },
                                                dismissButton = { TextButton(onClick = {}) { Text("Later") } },
                                                title = { Text("Background location") },
                                                text = { Text("To continue tracking runs while the app is backgrounded, allow background location access.") }
                                            )
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { backgroundPermissionState.launchPermissionRequest() }) {
                                                    Text("Background access")
                                                }
                                                OutlinedButton(onClick = {
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                                    context.startActivity(intent)
                                                }) {
                                                    Text("App settings")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                            Text("Grant Location Permission")
                        }
                    }
                }
            }

            if (latest != null) {
                item {
                    StatsCard(latest.distanceMeters, latest.durationSec, latest.avgSpeedMps, latest.calories)
                }

                item {
                    Card {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Route", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)) {
                                RunMapView(
                                    polylineEncoded = latest.polyline,
                                    replayPointIndex = if (replayPoints.isNotEmpty()) replayIndex else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (replayPoints.isNotEmpty()) {
                                Text("Replay", style = MaterialTheme.typography.titleSmall)
                                Slider(
                                    value = replayIndex.toFloat(),
                                    onValueChange = {
                                        replayPlaying = false
                                        replayIndex = it.toInt().coerceIn(0, max(replayPoints.lastIndex, 0))
                                    },
                                    valueRange = 0f..max(replayPoints.lastIndex, 0).toFloat()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Point ${replayIndex + 1} / ${replayPoints.size}")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            replayPlaying = false
                                            replayIndex = 0
                                        }) { Text("Reset") }
                                        Button(onClick = {
                                            if (replayIndex >= replayPoints.lastIndex) replayIndex = 0
                                            replayPlaying = !replayPlaying
                                        }) {
                                            Text(if (replayPlaying) "Pause Replay" else "Play Replay")
                                        }
                                    }
                                }
                            } else {
                                Text("Replay will appear once a route has been recorded.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(distanceMeters: Double, durationSec: Long, avgSpeedMps: Double, calories: Double) {
    val distanceKm = distanceMeters / 1000.0
    val paceSecPerKm = if (distanceMeters > 0.0) durationSec / (distanceMeters / 1000.0) else 0.0
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Live Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Distance", String.format(Locale.US, "%.2f km", distanceKm))
                StatItem("Time", formatDuration(durationSec))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Speed", String.format(Locale.US, "%.2f km/h", avgSpeedMps * 3.6))
                StatItem("Pace", if (paceSecPerKm > 0.0) formatPace(paceSecPerKm) else "--")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Calories", String.format(Locale.US, "%.0f kcal", calories))
                Spacer(modifier = Modifier.size(1.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

private fun formatPace(secPerKm: Double): String {
    val total = secPerKm.toLong()
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d /km".format(minutes, seconds)
}

