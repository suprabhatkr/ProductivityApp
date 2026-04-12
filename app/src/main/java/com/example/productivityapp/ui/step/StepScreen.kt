package com.example.productivityapp.ui.step

import androidx.compose.foundation.Canvas
// ...existing imports...
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
// ...existing imports...
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.service.StepCounterService
import com.example.productivityapp.viewmodel.StepViewModelFactory
import com.example.productivityapp.viewmodel.StepViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import java.time.LocalTime
// ...existing code...
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.defaultMinSize
// LocalContext not needed yet
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Step counter screen
 * - Top: pinned circular ring showing progress (doesn't scroll)
 * - Below: scrollable area with analysis, day-line (sparkline), past days
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StepScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideStepRepository(context)
    val userProfileRepo = RepositoryProvider.provideUserProfileRepository(context)
    val uiStateStore = RepositoryProvider.provideUiStateStore(context)
    val vm: StepViewModel = viewModel(factory = StepViewModelFactory(repo, userProfileRepo, uiStateStore))

    val steps by vm.steps.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val weekly by vm.weeklySteps.collectAsState()
    val distance by vm.distanceMeters.collectAsState()
    val serviceRunning by vm.serviceRunning.collectAsState()

    // simple derived values
    val progress = (if (goal > 0) steps.toFloat() / goal.toFloat() else 0f).coerceIn(0f, 1f)
    val calories = steps * 0.04
    val dayLine = weekly.ifEmpty { listOf(0, 0, 0, 0, 0, 0, 0) }

    val ringHeight = 300.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Scrollable content starts below the pinned ring — use padding top equal ring height
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = ringHeight)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                        // Start/Stop service controls (permission card kept inside the list)
                        item {
                            val permissionState = rememberPermissionState(Manifest.permission.ACTIVITY_RECOGNITION)
                            when (permissionState.status) {
                                is PermissionStatus.Granted -> {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (!serviceRunning) {
                                                        Button(onClick = {
                                                            // optimistically mark running to avoid duplicate clicks before UI recomposition
                                                            vm.setServiceRunning(true)
                                                            try {
                                                                val intent = Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_START }
                                                                context.startForegroundService(intent)
                                                            } catch (t: Throwable) {
                                                                vm.setServiceRunning(false) // revert on failure
                                                                Log.e("StepScreen", "Failed to start StepCounterService", t)
                                                                Toast.makeText(context, "Cannot start step service: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                                                            }
                                                        }) { Text("Start Service") }
                                            } else {
                                                        Button(onClick = {
                                                            try {
                                                                val intent = Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_STOP }
                                                                context.startService(intent)
                                                                vm.setServiceRunning(false)
                                                            } catch (t: Throwable) {
                                                                Log.e("StepScreen", "Failed to stop StepCounterService", t)
                                                                Toast.makeText(context, "Cannot stop step service: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                                                            }
                                                        }) { Text("Stop Service") }
                                            }
                                        }
                                    }
                                }
                                is PermissionStatus.Denied -> {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Activity recognition required", fontWeight = FontWeight.SemiBold)
                                            Text("This app needs Activity Recognition to read the step sensor and track steps in the background. Please grant permission to allow live step updates.")
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(modifier = Modifier.defaultMinSize(minWidth = 160.dp), onClick = { permissionState.launchPermissionRequest() }) { Text("Grant Activity Recognition") }
                                                OutlinedButton(onClick = {
                                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                                                    context.startActivity(intent)
                                                }) { Text("App Settings") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Today\'s Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Steps: $steps / $goal")
                            Text(String.format(Locale.US, "Avg pace: %d steps/hour", (if (distance > 0.0) (steps / (distance / 1000.0)).toInt() else (steps / 12))))
                            Text(String.format(Locale.US, "Calories: %.0f kcal", calories))
                        }
                    }
                }

                // Activity across the day (morning / afternoon / evening / night)
                item {
                    val morning = (steps * 30) / 100
                    val afternoon = (steps * 40) / 100
                    val evening = (steps * 20) / 100
                    val night = steps - (morning + afternoon + evening)
                    val segments = listOf(
                        "Morning" to morning,
                        "Afternoon" to afternoon,
                        "Evening" to evening,
                        "Night" to night
                    )

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Activity across the day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            DayActivityTimeline(segments = segments, modifier = Modifier.fillMaxWidth().height(56.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                segments.forEach { (label, value) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, style = MaterialTheme.typography.bodySmall)
                                        Text("$value", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Today\'s Line", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            DaySparkline(points = dayLine, modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp))
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Past days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Example list of past days
                val past = weekly.mapIndexed { idx, v -> "Day ${idx + 1} — $v" }.ifEmpty { listOf("No history yet") }
                items(past) { day ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(day)
                            Button(onClick = {}) { Text("Details") }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Pinned circular ring with compact service status under it
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(ringHeight)
                .align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StepRing(steps = steps, goal = goal, progress = progress)
                    Spacer(modifier = Modifier.height(12.dp))
                    // Compact service status row for quick access
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (serviceRunning) "Service running" else "Service stopped", style = MaterialTheme.typography.bodySmall)
                            if (!serviceRunning) {
                                Button(onClick = {
                                    try {
                                        val intent = Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_START }
                                        context.startForegroundService(intent)
                                        vm.setServiceRunning(true)
                                    } catch (t: Throwable) {
                                        Log.e("StepScreen", "Failed to start StepCounterService", t)
                                        Toast.makeText(context, "Cannot start step service: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }) { Text("Start") }
                            } else {
                                Button(onClick = {
                                    try {
                                        val intent = Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_STOP }
                                        context.startService(intent)
                                        vm.setServiceRunning(false)
                                    } catch (t: Throwable) {
                                        Log.e("StepScreen", "Failed to stop StepCounterService", t)
                                        Toast.makeText(context, "Cannot stop step service: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }) { Text("Stop") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRing(steps: Int, goal: Int, progress: Float) {
    val size = 220.dp
    // animate progress for smoother updates
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 600))

    val progressColor = MaterialTheme.colorScheme.primary

    val displayedSteps by animateIntAsState(targetValue = steps, animationSpec = tween(durationMillis = 600))

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).semantics { contentDescription = "Steps: $displayedSteps of $goal" }) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 20f, cap = StrokeCap.Round)
            // background ring
            drawArc(color = Color.LightGray, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            // progress (theme-aware color)
            drawArc(color = progressColor, startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false, style = stroke)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${displayedSteps}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("of $goal", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DaySparkline(points: List<Int>, modifier: Modifier = Modifier) {
    // Capture theme colors outside the Canvas (draw lambda is not a @Composable scope)
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = modifier.padding(8.dp)) {
        if (points.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val maxV = (points.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)
        if (points.size == 1) {
            // draw a single dot in the middle
            val x = w / 2f
            val y = h - (points[0] / maxV) * h
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
            return@Canvas
        }

        val stepX = w / (points.size - 1)
        for (i in 1 until points.size) {
            val x0 = (i - 1) * stepX
            val y0 = h - (points[i - 1] / maxV) * h
            val x1 = i * stepX
            val y1 = h - (points[i] / maxV) * h
            drawLine(color = lineColor, start = Offset(x0, y0), end = Offset(x1, y1), strokeWidth = 4f)
        }

        // draw a highlighted dot for the latest value
        val lastIndex = points.lastIndex
        val lx = lastIndex * stepX
        val ly = h - (points[lastIndex] / maxV) * h
        drawCircle(color = lineColor, radius = 8f, center = Offset(lx, ly))
        drawCircle(color = dotColor, radius = 4f, center = Offset(lx, ly))
    }
}


@Composable
private fun DayActivityTimeline(
    segments: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    showCurrentTimeIndicator: Boolean = true,
    pillRadius: Dp = 12.dp,
    gap: Dp = 6.dp
) {
    // compute values
    val values = segments.map { it.second }
    val maxV = values.maxOrNull() ?: 1
    val weights = if (values.sum() <= 0) segments.map { 1f } else values.map { it.toFloat() }

    val fillColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    // Increase visibility: larger pill height and stronger contrast
    val pillHeight = 28.dp
    val indicatorCanvasHeight = 40.dp

    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(pillHeight), verticalAlignment = Alignment.CenterVertically) {
            segments.forEachIndexed { idx, (_, value) ->
                val fillFraction = if (maxV > 0) value.toFloat() / maxV.toFloat() else 0f
                Box(modifier = Modifier
                    .weight(weights[idx])
                    .height(pillHeight)
                    .padding(end = if (idx < segments.lastIndex) gap else 0.dp)) {
                    // track (lighter background for contrast)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor.copy(alpha = 0.6f), shape = RoundedCornerShape(pillRadius)))

                    // fill (use solid primary for visibility)
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillFraction)
                        .background(fillColor, shape = RoundedCornerShape(pillRadius)))
                }
            }
        }

        if (showCurrentTimeIndicator) {
            val now = LocalTime.now()
            val minutesOfDay = now.hour * 60 + now.minute
            val fraction = minutesOfDay / (24f * 60f)

            val indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            val markerColor = MaterialTheme.colorScheme.primary

            // Draw indicator centered over the pill area
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(indicatorCanvasHeight)) {
                val x = size.width * fraction
                // position the line around the pill center
                val lineHeight = pillHeight.toPx() * 1.2f
                val top = (size.height - lineHeight) / 2f
                drawLine(color = indicatorColor, start = Offset(x, top), end = Offset(x, top + lineHeight), strokeWidth = 2.dp.toPx())
                // small marker circle placed at the pill center
                drawCircle(color = markerColor, radius = 6.dp.toPx(), center = Offset(x, size.height / 2f))
            }
        }
    }
}


