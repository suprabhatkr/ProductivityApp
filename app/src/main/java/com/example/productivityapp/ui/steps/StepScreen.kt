package com.example.productivityapp.ui.steps

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Scaffold
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.productivityapp.ui.theme.StepsAmber
import com.example.productivityapp.ui.theme.StepsAmberSoft
import com.example.productivityapp.ui.theme.TrackBeige
import com.example.productivityapp.ui.theme.StepsSurface
import com.example.productivityapp.ui.theme.StepsBackground
import com.example.productivityapp.ui.theme.BlendLight
import com.example.productivityapp.ui.theme.BlendPrimaryDark
import com.example.productivityapp.ui.theme.BlendDarkBackground
import com.example.productivityapp.ui.theme.TextPrimary
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.service.StepCounterService
import com.example.productivityapp.viewmodel.StepViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.Alignment
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun StatIcon(label: String, modifier: Modifier = Modifier) {
    val image = when (label) {
        "Active" -> Icons.Filled.FitnessCenter
        "Distance" -> Icons.Filled.LocationOn
        "Active time" -> Icons.Filled.Timer
        else -> Icons.Filled.Info
    }
    Icon(imageVector = image, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = modifier)
}

enum class StepPermissionUiState {
    Granted,
    RationaleRequired,
    RequestOrSettings,
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StepScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideStepRepository(context)
    val userProfileRepo = RepositoryProvider.provideUserProfileRepository(context)
    val uiStateStore = com.example.productivityapp.data.RepositoryProvider.provideUiStateStore(context)
    val vm: com.example.productivityapp.viewmodel.StepViewModel = viewModel(
        factory = com.example.productivityapp.viewmodel.StepViewModelFactory(repo, userProfileRepo, uiStateStore)
    )
    // UI-running flag is restored by StepViewModel from UiStateStore
    val steps = vm.steps.collectAsState()
    val dailyGoal = vm.dailyGoal.collectAsState()
    val serviceRunning = vm.serviceRunning.collectAsState()
    val weeklyStepsState = vm.weeklySteps.collectAsState()
    val permissionState = rememberPermissionState(Manifest.permission.ACTIVITY_RECOGNITION)
    val sensorManager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val hasStepSensor = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null }

    val permissionUiState = when (val status = permissionState.status) {
        is PermissionStatus.Granted -> StepPermissionUiState.Granted
        is PermissionStatus.Denied -> if (status.shouldShowRationale) {
            StepPermissionUiState.RationaleRequired
        } else {
            StepPermissionUiState.RequestOrSettings
        }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val startInFlightState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val todaySegmentsState = vm.todaySegments.collectAsState()

    StepScreenContent(
        steps = steps.value,
        todaySegments = todaySegmentsState.value,
        dailyGoal = dailyGoal.value,
        weeklySteps = weeklyStepsState.value,
        serviceRunning = serviceRunning.value,
        permissionUiState = permissionUiState,
        hasStepSensor = hasStepSensor,
        onAddManualSteps = { vm.addManualSteps(it) },
        onStartService = {
            if (startInFlightState.value) return@StepScreenContent
            // Ensure activity recognition permission is granted before starting the foreground service.
            if (permissionState.status is PermissionStatus.Granted) {
                startInFlightState.value = true
                // prevent double-clicks by setting running state first; revert on failure
                Log.i("StepScreen", "Start requested (compose)")
                vm.setServiceRunning(true)
                try {
                    val intent = Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_START }
                    ContextCompat.startForegroundService(context, intent)
                    try {
                        val prefs = context.applicationContext.getSharedPreferences("step_service_prefs", Context.MODE_PRIVATE)
                        val editor = prefs.edit() as android.content.SharedPreferences.Editor
                        editor.putBoolean("ui_service_running", true)
                        editor.apply()
                    } catch (_: Throwable) {}
                } catch (t: Throwable) {
                    vm.setServiceRunning(false)
                    startInFlightState.value = false
                    permissionState.launchPermissionRequest()
                }
                coroutineScope.launch { kotlinx.coroutines.delay(500); if (!serviceRunning.value) startInFlightState.value = false }
            } else {
                // Request permission; UI will update and show rationale/settings flow
                permissionState.launchPermissionRequest()
            }
        },



        onStopService = {
            val intent = Intent(context, StepCounterService::class.java).apply { action = StepCounterService.ACTION_STOP }
            context.startService(intent)
            vm.setServiceRunning(false)
            try {
                val prefs = context.applicationContext.getSharedPreferences("step_service_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit() as android.content.SharedPreferences.Editor
                editor.putBoolean("ui_service_running", false)
                editor.apply()
            } catch (_: Throwable) {}
        },
        onRequestPermission = { permissionState.launchPermissionRequest() },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            context.startActivity(intent)
        },
        onBack = onBack,
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun StepScreenContent(
    steps: Int,
    dailyGoal: Int,
    weeklySteps: List<Int> = emptyList(),
    todaySegments: List<Pair<String, Int>> = emptyList(),
    serviceRunning: Boolean,
    permissionUiState: StepPermissionUiState,
    hasStepSensor: Boolean,
    onAddManualSteps: (Int) -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit = {},
) {
    var customManualSteps by rememberSaveable { mutableStateOf("") }
    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val stepCardBg = if (isDarkMode) BlendPrimaryDark else BlendLight
    val contentBg = MaterialTheme.colorScheme.background
    val topBarBg = if (isDarkMode) BlendPrimaryDark else BlendLight
    val topBarTitleColor = if (isDarkMode) Color.White else TextPrimary
    val weekly = if (weeklySteps.isNotEmpty()) weeklySteps else remember(steps, dailyGoal) {
        val list = MutableList(7) { i ->
            val factor = 0.4f + (i * 0.09f)
            (dailyGoal * factor).toInt().coerceAtMost(dailyGoal)
        }
        list[list.lastIndex] = steps
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 18.sp, color = topBarTitleColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = topBarTitleColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg,
                    titleContentColor = topBarTitleColor
                ),
                modifier = Modifier.height(85.dp)
            )
        },
        containerColor = contentBg
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = contentBg
        ) {
            val ringHeight = 280.dp
            val ringTopOffset = 16.dp

            Box(modifier = Modifier.fillMaxSize()) {
                // Scrollable content below the pinned ring
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = ringHeight + ringTopOffset)
                        .padding(com.example.productivityapp.ui.theme.Spacing.large),
                    verticalArrangement = Arrangement.spacedBy(com.example.productivityapp.ui.theme.Spacing.large),
                ) {
                    item {
                        // Today's activity breakdown (morning / afternoon / evening)
                        val segmentsToShow = if (todaySegments.isNotEmpty()) todaySegments else listOf(
                            "Morning" to (steps * 0.35f).toInt(),
                            "Afternoon" to (steps * 0.30f).toInt(),
                            "Evening" to (steps * 0.35f).toInt()
                        )
                        TodayActivity(
                            segments = segmentsToShow
                        )
                    }

                    item {
                        // Small analytics row: calories, distance, active time
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = stepCardBg)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) com.example.productivityapp.ui.theme.BlendPrimaryDark else MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        StatIcon("Active", modifier = Modifier.size(28.dp))
                                        Text("Active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${(steps*0.02).toInt()} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) com.example.productivityapp.ui.theme.BlendPrimaryDark else MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        StatIcon("Distance", modifier = Modifier.size(28.dp))
                                        Text("Distance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(String.format("%.2f km", steps * 0.0008), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) com.example.productivityapp.ui.theme.BlendPrimaryDark else MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        StatIcon("Active time", modifier = Modifier.size(28.dp))
                                        Text("Active time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${(steps/100).coerceAtLeast(0)} min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        StepsWeeklyChart(values = weekly, dailyGoal = dailyGoal)
                    }


                    item {
                        if (!hasStepSensor) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.example.productivityapp.ui.theme.StepsAmber)) {
                                Column(modifier = Modifier.padding(com.example.productivityapp.ui.theme.Spacing.large), verticalArrangement = Arrangement.spacedBy(com.example.productivityapp.ui.theme.Spacing.small)) {
                                    Text("Automatic step tracking unavailable", style = MaterialTheme.typography.titleMedium)
                                    Text("This device does not provide a hardware step counter sensor. You can still log steps manually.")
                                    Button(onClick = { showManualDialog = true }, modifier = Modifier.semantics { testTag = "add_manual_button"; contentDescription = "Add steps manually" }) {
                                        Text("Add Manually")
                                    }
                                }
                            }
                        } else {
                            when (permissionUiState) {
                                StepPermissionUiState.Granted -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                if (serviceRunning) onStopService() else onStartService()
                                            },
                                            modifier = Modifier.semantics { testTag = "step_service_toggle"; contentDescription = if (!serviceRunning) "Start automatic step tracking" else "Stop automatic step tracking" },
                                        ) {
                                            Text(if (!serviceRunning) "Start Step Service" else "Stop Step Service")
                                        }
                                    }
                                }

                                StepPermissionUiState.RationaleRequired -> {
                                    AlertDialog(
                                        onDismissRequest = {},
                                        confirmButton = {
                                            TextButton(onClick = onRequestPermission) {
                                                Text("Allow")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = {}) {
                                                Text("Use manual entry")
                                            }
                                        },
                                        title = { Text("Activity recognition") },
                                        text = { Text("This app needs activity recognition to count your steps automatically. You can continue using manual entry if you prefer.") },
                                    )
                                }

                                StepPermissionUiState.RequestOrSettings -> {
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = stepCardBg)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Automatic tracking permission", style = MaterialTheme.typography.titleMedium)
                                            Text("Grant activity recognition to count steps automatically, or continue with manual entry.")
                                            Button(onClick = onRequestPermission, modifier = Modifier.semantics { testTag = "request_permission_button"; contentDescription = "Request activity recognition permission" }) {
                                                Text("Grant Activity Recognition")
                                            }
                                            Spacer(modifier = Modifier.height(com.example.productivityapp.ui.theme.Spacing.tiny))
                                            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.semantics { testTag = "open_settings_button"; contentDescription = "Open app settings" }) {
                                                Text("Open app settings")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pinned circular ring at the top
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(ringHeight)
                    .align(Alignment.TopCenter)) {

                    // compute trend compared to previous day (left side)
                    val prev = if (weekly.size >= 2) weekly[weekly.lastIndex - 1] else 0
                    val delta = steps - prev
                    val (trendText, trendColor) = if (prev > 0) {
                        val pct = ((delta.toDouble() / prev.toDouble()) * 100.0).toInt()
                        if (delta >= 0) ("${kotlin.math.abs(pct)}% up ↑" to Color(0xFF2ECC71)) else ("${kotlin.math.abs(pct)}% down ↓" to Color(0xFFEF4444))
                    } else {
                        if (steps > 0) ("New" to Color(0xFF2ECC71)) else ("0%" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally) {

                        // ring centered
                        Box(contentAlignment = Alignment.Center) {
                            StepRing(
                                steps = steps,
                                goal = dailyGoal,
                                progress = if (dailyGoal > 0) (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f,1f) else 0f,
                                modifier = Modifier.padding(top = ringTopOffset)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Row below the ring for trend (left) and add button (right)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.padding(start = 24.dp, top = 0.dp).offset(y = (-6).dp)) {
                                Text(trendText, color = trendColor, style = MaterialTheme.typography.bodyLarge)
                            }
                            Box(modifier = Modifier.padding(end = 32.dp, top = 0.dp).offset(y = (-6).dp)) {
                                AddStepsFab(onClick = { showManualDialog = true })
                            }
                        }
                    }

                    // Manual entry dialog (popup)
                    if (showManualDialog) {
                        ManualEntryDialog(show = true, onDismiss = { showManualDialog = false }, onAdd = { amount ->
                            onAddManualSteps(amount)
                            showManualDialog = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRing(steps: Int, goal: Int, progress: Float, modifier: Modifier = Modifier) {
    val size = 240.dp
    val isDarkLocal = androidx.compose.foundation.isSystemInDarkTheme()
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        // No background glow — draw only the ring (blades removed)
        Box(modifier = Modifier.size(size)) { /* intentionally empty */ }

        Canvas(modifier = Modifier.size(size)) {
            // subtle background track (mode-aware)
            val bgStroke = Stroke(width = 36f, cap = StrokeCap.Round)
            val unfilledColor = if (isDarkLocal) BlendPrimaryDark else TrackBeige
            drawArc(color = unfilledColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = bgStroke)
            // main progress arc (filled)
            val stroke = Stroke(width = 28f, cap = StrokeCap.Round)
            drawArc(color = StepsAmber, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = stroke)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$steps (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    shadow = Shadow(color = StepsAmberSoft, blurRadius = 16f)
                )
            )
            Text(
                "of $goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
