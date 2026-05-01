package com.example.productivityapp.ui.run

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.service.RunTrackingService
import com.example.productivityapp.ui.settings.RunFeatureSettingsDialog
import com.example.productivityapp.ui.settings.rememberSharedSettingsViewModel
import com.example.productivityapp.viewmodel.RunViewModel
import com.example.productivityapp.viewmodel.RunViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal val RunBackdropLight = Color(0xFFF7F2FF)
internal val RunBackdropDark = Color(0xFF120D1E)
internal val RunSurfaceLight = Color(0xFFEDE2FF)
internal val RunSurfaceDark = Color(0xFF211635)
internal val RunSurfaceAltLight = Color(0xFFF7F0FF)
internal val RunSurfaceAltDark = Color(0xFF1A122B)
internal val RunTrackLight = Color(0xFFD8C8F7)
internal val RunTrackDark = Color(0xFF34254D)
internal val RunAccentLight = Color(0xFF7B4DDB)
internal val RunAccentDark = Color(0xFFC3A8FF)
internal val RunToneLight = Color(0xFF8C72C0)
internal val RunToneDark = Color(0xFFD6C9F5)
internal val RunChipLight = Color(0xFFE7DBFF)
internal val RunChipDark = Color(0xFF2A1D42)
internal val RunWarningLight = Color(0xFFE46A53)
internal val RunWarningDark = Color(0xFFFFA996)

internal const val DefaultDailyRunGoalMeters = 5_000.0
private val RunDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val RunTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@OptIn(ExperimentalPermissionsApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    onBack: () -> Unit = {},
    onOpenRunDetails: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val shouldPromptNotificationPermission = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    val shouldShowBackgroundPermissionCard = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q }
    val shouldOpenBackgroundSettings = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R }
    val repo = RepositoryProvider.provideRunRepository(context)
    val uiStateStore = RepositoryProvider.provideUiStateStore(context)
    val vm: RunViewModel = viewModel(factory = RunViewModelFactory(repo, uiStateStore))
    val runs = vm.runs.collectAsState()
    val latest = runs.value.firstOrNull()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val backgroundPermissionState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    val permissionCoordinator = remember { RunPermissionCoordinator() }

    val runningState = vm.uiRunning.collectAsState()
    var running by rememberSaveable { mutableStateOf(runningState.value) }
    LaunchedEffect(runningState.value) { running = runningState.value }
    val settingsViewModel = rememberSharedSettingsViewModel()
    val settingsUiState = settingsViewModel.uiState.collectAsState()
    var showFeatureSettings by rememberSaveable { mutableStateOf(false) }

    val hasPausedRun = hasPausedRun(latest, running)
    val permissionUiState = remember(
        running,
        hasPausedRun,
        locationPermissionState.status,
        notificationPermissionState.status,
        backgroundPermissionState.status,
    ) {
        permissionCoordinator.buildUiState(
            RunPermissionContext(
                hasLocationPermission = locationPermissionState.status is PermissionStatus.Granted,
                hasNotificationPermission = !shouldPromptNotificationPermission ||
                    notificationPermissionState.status is PermissionStatus.Granted,
                hasBackgroundPermission = !shouldShowBackgroundPermissionCard ||
                    backgroundPermissionState.status is PermissionStatus.Granted,
                shouldShowBackgroundRationale = (backgroundPermissionState.status as? PermissionStatus.Denied)
                    ?.shouldShowRationale == true && !shouldOpenBackgroundSettings,
                shouldPromptNotificationPermission = shouldPromptNotificationPermission,
                shouldShowBackgroundPermissionCard = shouldShowBackgroundPermissionCard,
                shouldOpenBackgroundSettings = shouldOpenBackgroundSettings,
                isTracking = running,
                hasPausedRun = hasPausedRun,
            )
        )
    }

    RunScreenContent(
        runs = runs.value,
        isTracking = running,
        permissionUiState = permissionUiState,
        onOpenAppSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        },
        onPrimaryRunAction = {
            val intent = Intent(context, RunTrackingService::class.java)
            when (permissionUiState.primaryAction) {
                RunPrimaryAction.REQUEST_LOCATION -> {
                    locationPermissionState.launchPermissionRequest()
                }
                RunPrimaryAction.START_OR_RESUME_RUN -> {
                    if (permissionUiState.shouldRequestNotificationsBeforeTracking) {
                        notificationPermissionState.launchPermissionRequest()
                    }
                    intent.action = RunTrackingService.ACTION_START
                    context.startForegroundService(intent)
                    vm.setUiRunning(true)
                }
                RunPrimaryAction.STOP_RUN -> {
                    intent.action = RunTrackingService.ACTION_STOP
                    context.startService(intent)
                    vm.setUiRunning(false)
                }
            }
        },
        onPauseRun = {
            context.startService(
                Intent(context, RunTrackingService::class.java).apply {
                    action = RunTrackingService.ACTION_PAUSE
                }
            )
            vm.setUiRunning(false)
        },
        onResumeRun = {
            if (permissionUiState.shouldRequestNotificationsBeforeTracking) {
                notificationPermissionState.launchPermissionRequest()
            }
            context.startService(
                Intent(context, RunTrackingService::class.java).apply {
                    action = RunTrackingService.ACTION_RESUME
                }
            )
            vm.setUiRunning(true)
        },
        onPermissionCardAction = { action ->
            when (action) {
                RunPermissionCardAction.REQUEST_LOCATION -> locationPermissionState.launchPermissionRequest()
                RunPermissionCardAction.REQUEST_NOTIFICATIONS -> notificationPermissionState.launchPermissionRequest()
                RunPermissionCardAction.REQUEST_BACKGROUND -> backgroundPermissionState.launchPermissionRequest()
                RunPermissionCardAction.OPEN_BACKGROUND_SETTINGS -> {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                }
            }
        },
        onOpenRunDetails = onOpenRunDetails,
        onOpenFeatureSettings = { showFeatureSettings = true },
        onBack = onBack,
    )

    if (showFeatureSettings) {
        RunFeatureSettingsDialog(
            uiState = settingsUiState.value,
            onDismiss = { showFeatureSettings = false },
            onPreferredUnitsChanged = settingsViewModel::updatePreferredUnits,
            onSave = {
                if (settingsViewModel.saveSettings()) {
                    showFeatureSettings = false
                }
            },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun RunScreenContent(
    runs: List<RunEntity>,
    isTracking: Boolean,
    permissionUiState: RunPermissionUiState,
    onOpenAppSettings: () -> Unit,
    onPrimaryRunAction: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onPermissionCardAction: (RunPermissionCardAction) -> Unit,
    onOpenRunDetails: (Long) -> Unit = {},
    onOpenFeatureSettings: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val snapshot = remember(runs, isTracking) {
        buildRunDashboardSnapshot(runs = runs, isTracking = isTracking)
    }
    val latest = runs.firstOrNull()
    val latestCompleted = runs.firstOrNull { it.endTime != null }
    val hasPausedRun = hasPausedRun(latest, isTracking)
    val darkTheme = isSystemInDarkTheme()
    val backdrop = if (darkTheme) RunBackdropDark else RunBackdropLight
    val surface = if (darkTheme) RunSurfaceDark else RunSurfaceLight
    val surfaceAlt = if (darkTheme) RunSurfaceAltDark else RunSurfaceAltLight
    val track = if (darkTheme) RunTrackDark else RunTrackLight
    val accent = if (darkTheme) RunAccentDark else RunAccentLight
    val tone = if (darkTheme) RunToneDark else RunToneLight
    val chipColor = if (darkTheme) RunChipDark else RunChipLight
    val warning = if (darkTheme) RunWarningDark else RunWarningLight

    val metrics = remember(snapshot) {
        listOf(
            RunMetric(
                title = "This week",
                value = formatDistance(snapshot.weeklyDistanceMeters),
                subtitle = "${snapshot.weeklyRunCount} run${if (snapshot.weeklyRunCount == 1) "" else "s"}"
            ),
            RunMetric(
                title = "Total runs",
                value = snapshot.totalRuns.toString(),
                subtitle = "${snapshot.completedRuns} completed"
            ),
            RunMetric(
                title = "Best pace",
                value = snapshot.bestPaceLabel ?: "--",
                subtitle = "Fastest recent pace"
            ),
            RunMetric(
                title = "Longest",
                value = formatDistance(snapshot.longestRunMeters),
                subtitle = "Longest recorded run"
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenFeatureSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Open run settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                expandedHeight = 48.dp,
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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    RunHeroRingCard(
                        snapshot = snapshot,
                        accent = accent,
                        track = track,
                        tone = tone,
                        surface = surface,
                        chipColor = chipColor,
                    )
                }

                item {
                    RunActionCard(
                        snapshot = snapshot,
                        primaryLabel = permissionUiState.primaryActionLabel,
                        accent = accent,
                        surface = surfaceAlt,
                        warning = warning,
                        onPrimaryAction = onPrimaryRunAction,
                        onResumeRun = onResumeRun,
                        onPauseRun = onPauseRun,
                    )
                }

                permissionUiState.permissionCard?.let { card ->
                    item {
                        RunPermissionCard(
                            title = card.title,
                            message = card.message,
                            primaryLabel = card.primaryLabel,
                            secondaryLabel = card.secondaryLabel,
                            surface = surface,
                            accent = accent,
                            onPrimary = { onPermissionCardAction(card.action) },
                            onSecondary = onOpenAppSettings,
                        )
                    }
                }

                item {
                    RunLatestRouteCard(
                        latestRun = latest,
                        accent = accent,
                        surface = surfaceAlt,
                        onOpenRunDetails = onOpenRunDetails,
                    )
                }

                items(metrics.chunked(2)) { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowMetrics.forEach { metric ->
                            RunMetricCard(
                                metric = metric,
                                surface = surface,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowMetrics.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    RunInsightsCard(
                        snapshot = snapshot,
                        latestCompletedRun = latestCompleted,
                        surface = surface,
                        accent = accent,
                    )
                }

                item {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (runs.isEmpty()) {
                    item {
                        RunEmptyHistoryCard(surface = surfaceAlt)
                    }
                } else {
                    items(runs.take(6), key = { it.id }) { run ->
                        RunHistoryCard(
                            run = run,
                            isTracking = isTracking && run.id == latest?.id,
                            isPaused = !isTracking && run.id == latest?.id && run.endTime == null,
                            surface = surfaceAlt,
                            accent = accent,
                            tone = tone,
                            warning = warning,
                            onOpenRunDetails = onOpenRunDetails,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunHeroRingCard(
    snapshot: RunDashboardSnapshot,
    accent: Color,
    track: Color,
    tone: Color,
    surface: Color,
    chipColor: Color,
) {
    val isDark = isSystemInDarkTheme()
    val headlineSurface = if (isDark) surface.copy(alpha = 0.96f) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = headlineSurface),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = "Today's run - ${snapshot.statusLabel}",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        RunProgressRing(
            progress = snapshot.todayGoalProgress,
            totalLabel = formatDistance(snapshot.todayDistanceMeters),
            goalLabel = formatDistance(snapshot.dailyGoalMeters),
            accent = accent,
            track = track,
            background = surface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RunChip(
                text = "This week ${formatDistance(snapshot.weeklyDistanceMeters)}",
                background = chipColor,
                color = tone,
            )
            RunChip(
                text = "${snapshot.totalRuns} run${if (snapshot.totalRuns == 1) "" else "s"}",
                background = chipColor,
                color = tone,
            )
        }
    }
}

@Composable
private fun RunActionCard(
    snapshot: RunDashboardSnapshot,
    primaryLabel: String,
    accent: Color,
    surface: Color,
    warning: Color,
    onPrimaryAction: () -> Unit,
    onResumeRun: () -> Unit,
    onPauseRun: () -> Unit,
) {
    val stateAccent = when {
        snapshot.isTracking -> accent
        snapshot.hasPausedRun -> warning
        else -> accent
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(stateAccent)
                )
                Text("Run controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                
            }
            Text(
                snapshot.statusDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RunChip(
                    text = snapshot.statusLabel,
                    background = stateAccent.copy(alpha = 0.14f),
                    color = stateAccent,
                )
                Text(
                    if (snapshot.isTracking) "Tracking live" else "Scroll for route & history",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = primaryLabel },
                ) {
                    Text(primaryLabel)
                }
                if (snapshot.isTracking) {
                    OutlinedButton(
                        onClick = onPauseRun,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Pause run" },
                    ) {
                        Text("Pause")
                    }
                } else if (snapshot.hasPausedRun) {
                    OutlinedButton(
                        onClick = onResumeRun,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Resume run" },
                    ) {
                        Text("Resume")
                    }
                }
            }
        }
    }
}

@Composable
private fun RunPermissionCard(
    title: String,
    message: String,
    primaryLabel: String,
    secondaryLabel: String,
    surface: Color,
    accent: Color,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.semantics { contentDescription = primaryLabel },
                ) {
                    Text(primaryLabel)
                }
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.semantics { contentDescription = secondaryLabel },
                ) {
                    Text(secondaryLabel)
                }
            }
            Text(
                "You can keep browsing runs even if you skip this for now.",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun RunLatestRouteCard(
    latestRun: RunEntity?,
    accent: Color,
    surface: Color,
    onOpenRunDetails: (Long) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (latestRun?.endTime == null && latestRun != null) "Live route" else "Latest route",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            when {
                latestRun == null -> {
                    Text(
                        "Your most recent route will appear here once you log a run.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                latestRun.polyline.isBlank() -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                formatDistance(latestRun.distanceMeters),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                formatDuration(latestRun.durationSec),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenRunDetails(latestRun.id) },
                            modifier = Modifier.semantics { contentDescription = "Open latest run details" },
                        ) {
                            Text("Open details")
                        }
                    }
                    Text(
                        "Route points will appear once movement is captured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                formatDistance(latestRun.distanceMeters),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                formatDuration(latestRun.durationSec),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenRunDetails(latestRun.id) },
                            modifier = Modifier.semantics { contentDescription = "Open latest run details" },
                        ) {
                            Text("Open details")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.08f)),
                    ) {
                        RunMapView(
                            polylineEncoded = latestRun.polyline,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Text(
                        if (latestRun.endTime == null) {
                            "Open the details screen for a dedicated route view and the upcoming replay, analysis, and sharing surface."
                        } else {
                            "Open details for the full route view, replay controls, and analysis shell."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RunMetricCard(
    metric: RunMetric,
    surface: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(metric.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                metric.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(metric.subtitle, style = MaterialTheme.typography.bodySmall, color = accent)
        }
    }
}

@Composable
private fun RunInsightsCard(
    snapshot: RunDashboardSnapshot,
    latestCompletedRun: RunEntity?,
    surface: Color,
    accent: Color,
) {
    val insight = when {
        snapshot.isTracking -> "You are currently recording a run. Keep the app visible or enable background access when you want more reliable tracking outside the app."
        snapshot.hasPausedRun -> "You have a paused run waiting. Resume it when you're ready, or stop it to save the session."
        latestCompletedRun != null -> "Your latest completed run covered ${formatDistance(latestCompletedRun.distanceMeters)} in ${formatDuration(latestCompletedRun.durationSec)}."
        else -> "Start your first run to unlock route history, replay controls, and deeper analysis on this dashboard."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Run insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Scroll to review your latest route, summary metrics, and recent sessions in one place.",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun RunHistoryCard(
    run: RunEntity,
    isTracking: Boolean,
    isPaused: Boolean,
    surface: Color,
    accent: Color,
    tone: Color,
    warning: Color,
    onOpenRunDetails: (Long) -> Unit,
) {
    val stateText = when {
        isTracking -> "Tracking"
        isPaused -> "Paused"
        else -> "Completed"
    }
    val stateColor = when {
        isTracking -> accent
        isPaused -> warning
        else -> tone
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenRunDetails(run.id) }
            .semantics { contentDescription = "Open run details for ${formatRunDate(run.startTime)}" },
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        formatRunDate(run.startTime),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        formatRunTime(run.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RunChip(
                    text = stateText,
                    background = stateColor.copy(alpha = 0.14f),
                    color = stateColor,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("Distance", formatDistance(run.distanceMeters))
                StatItem("Time", formatDuration(run.durationSec))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("Speed", String.format(Locale.US, "%.2f km/h", run.avgSpeedMps * 3.6))
                StatItem(
                    "Pace",
                    runPaceLabel(run) ?: "--",
                )
            }
        }
    }
}

@Composable
private fun RunEmptyHistoryCard(surface: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            "No runs yet. When you finish your first route, it will appear here with quick stats and map context.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun RunChip(
    text: String,
    background: Color,
    color: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RunProgressRing(
    progress: Float,
    totalLabel: String,
    goalLabel: String,
    accent: Color,
    track: Color,
    background: Color,
) {
    val isDark = isSystemInDarkTheme()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(260.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        background,
                        if (isDark) background.copy(alpha = 0.92f) else Color.White,
                    )
                )
            )
            .padding(14.dp)
            .semantics { contentDescription = "Run progress ring" },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 28f
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = size.minDimension * 0.38f,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                totalLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "of $goalLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}% of today's default goal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class RunMetric(
    val title: String,
    val value: String,
    val subtitle: String,
)

internal data class RunDashboardSnapshot(
    val todayDistanceMeters: Double,
    val weeklyDistanceMeters: Double,
    val weeklyRunCount: Int,
    val totalRuns: Int,
    val completedRuns: Int,
    val dailyGoalMeters: Double,
    val todayGoalProgress: Float,
    val bestPaceLabel: String?,
    val longestRunMeters: Double,
    val statusLabel: String,
    val statusDetail: String,
    val isTracking: Boolean,
    val hasPausedRun: Boolean,
)

internal fun buildRunDashboardSnapshot(
    runs: List<RunEntity>,
    isTracking: Boolean,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId),
    dailyGoalMeters: Double = DefaultDailyRunGoalMeters,
): RunDashboardSnapshot {
    val weeklyStart = today.minusDays(6)
    val todayRuns = runs.filter { runDate(it, zoneId) == today }
    val weeklyRuns = runs.filter { runDate(it, zoneId) >= weeklyStart }
    val completedRuns = runs.filter { it.endTime != null }
    val latest = runs.firstOrNull()
    val hasPausedRun = hasPausedRun(latest, isTracking)

    val todayDistance = todayRuns.sumOf { it.distanceMeters }
    val weeklyDistance = weeklyRuns.sumOf { it.distanceMeters }
    val bestPaceSecPerKm = completedRuns
        .mapNotNull { runPaceSecPerKm(it) }
        .minOrNull()

    val statusLabel = when {
        isTracking -> "Run in progress"
        hasPausedRun -> "Run paused"
        completedRuns.isNotEmpty() -> "Ready for your next run"
        else -> "Ready to start"
    }
    val statusDetail = when {
        isTracking -> "Distance and route will keep updating while this session is active."
        hasPausedRun -> "Pick up where you left off, or stop the current run to save it."
        completedRuns.isNotEmpty() -> "Review your latest route, then head back out when you're ready."
        else -> "Start a run to begin building route history, pace context, and replayable sessions."
    }

    return RunDashboardSnapshot(
        todayDistanceMeters = todayDistance,
        weeklyDistanceMeters = weeklyDistance,
        weeklyRunCount = weeklyRuns.size,
        totalRuns = runs.size,
        completedRuns = completedRuns.size,
        dailyGoalMeters = dailyGoalMeters,
        todayGoalProgress = (todayDistance / dailyGoalMeters).toFloat().coerceIn(0f, 1f),
        bestPaceLabel = bestPaceSecPerKm?.let(::formatPace),
        longestRunMeters = completedRuns.maxOfOrNull { it.distanceMeters } ?: 0.0,
        statusLabel = statusLabel,
        statusDetail = statusDetail,
        isTracking = isTracking,
        hasPausedRun = hasPausedRun,
    )
}

internal fun runDate(run: RunEntity, zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(run.startTime).atZone(zoneId).toLocalDate()

internal fun hasPausedRun(latest: RunEntity?, isTracking: Boolean): Boolean =
    latest != null && latest.endTime == null && !isTracking

internal fun formatRunDate(timestampMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate().format(RunDateFormatter)

internal fun formatRunTime(timestampMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalTime().format(RunTimeFormatter)

internal fun formatDistance(distanceMeters: Double): String =
    String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)

internal fun runPaceSecPerKm(run: RunEntity): Double? =
    if (run.distanceMeters > 0.0 && run.durationSec > 0L) run.durationSec / (run.distanceMeters / 1000.0) else null

internal fun runPaceLabel(run: RunEntity): String? = runPaceSecPerKm(run)?.let(::formatPace)

internal fun formatDuration(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

internal fun formatPace(secPerKm: Double): String {
    val total = secPerKm.toLong()
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d /km".format(minutes, seconds)
}
