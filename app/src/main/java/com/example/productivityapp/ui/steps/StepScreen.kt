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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

    StepScreenContent(
        steps = steps.value,
        dailyGoal = dailyGoal.value,
        weeklySteps = weeklyStepsState.value,
        serviceRunning = serviceRunning.value,
        permissionUiState = permissionUiState,
        hasStepSensor = hasStepSensor,
        onAddManualSteps = { vm.addManualSteps(it) },
        onStartService = {
            // Ensure activity recognition permission is granted before starting the foreground service.
            if (permissionState.status is PermissionStatus.Granted) {
                // prevent double-clicks by setting running state first; revert on failure
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
                    permissionState.launchPermissionRequest()
                }
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
    val goalProgress = if (dailyGoal > 0) (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 0f
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
                title = { Text("Steps", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = Color.White, fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val ringHeight = 300.dp

            Box(modifier = Modifier.fillMaxSize()) {
                // Scrollable content below the pinned ring
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = ringHeight)
                        .padding(com.example.productivityapp.ui.theme.Spacing.large),
                    verticalArrangement = Arrangement.spacedBy(com.example.productivityapp.ui.theme.Spacing.large),
                ) {
                    item {
                        StepsProgress(steps = steps, dailyGoal = dailyGoal)
                    }

                    item {
                        StepsWeeklyChart(values = weekly, dailyGoal = dailyGoal)
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.example.productivityapp.ui.theme.StepsAmber)) {
                            Column(modifier = Modifier.padding(com.example.productivityapp.ui.theme.Spacing.large), verticalArrangement = Arrangement.spacedBy(com.example.productivityapp.ui.theme.Spacing.medium)) {
                                Text("Manual entry", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onAddManualSteps(100) },
                                            modifier = Modifier.semantics { testTag = "quick_add_100"; contentDescription = "Add one hundred steps" }
                                        ) {
                                            Text("+100")
                                        }
                                        OutlinedButton(
                                            onClick = { onAddManualSteps(500) },
                                            modifier = Modifier.semantics { testTag = "quick_add_500"; contentDescription = "Add five hundred steps" }
                                        ) {
                                            Text("+500")
                                        }
                                }
                                OutlinedTextField(
                                    value = customManualSteps,
                                    onValueChange = { value -> customManualSteps = value.filter { it.isDigit() }.take(5) },
                                    label = { Text("Custom steps") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { testTag = "custom_step_input"; contentDescription = "Custom steps input" },
                                )
                                Button(
                                    onClick = {
                                        customManualSteps.toIntOrNull()?.takeIf { it > 0 }?.let(onAddManualSteps)
                                        customManualSteps = ""
                                    },
                                    modifier = Modifier.semantics { testTag = "custom_step_submit"; contentDescription = "Submit custom steps" },
                                ) {
                                    Text("Add custom steps")
                                }
                            }
                        }
                    }

                    item {
                        if (!hasStepSensor) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.example.productivityapp.ui.theme.StepsAmber)) {
                                Column(modifier = Modifier.padding(com.example.productivityapp.ui.theme.Spacing.large), verticalArrangement = Arrangement.spacedBy(com.example.productivityapp.ui.theme.Spacing.small)) {
                                    Text("Automatic step tracking unavailable", style = MaterialTheme.typography.titleMedium)
                                    Text("This device does not provide a hardware step counter sensor. You can still log steps manually.")
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
                                    Card(modifier = Modifier.fillMaxWidth()) {
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
                    .align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
                    StepRing(steps = steps, goal = dailyGoal, progress = if (dailyGoal > 0) (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f,1f) else 0f)
                }
            }
        }
    }
}

@Composable
private fun StepRing(steps: Int, goal: Int, progress: Float) {
    val size = 220.dp
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 20f, cap = StrokeCap.Round)
            drawArc(color = Color.LightGray, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = Color(0xFF6A1B9A), startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = stroke)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${steps}", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("of $goal", style = MaterialTheme.typography.bodySmall)
        }
    }
}
