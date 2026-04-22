package com.example.productivityapp.ui.sleep

import android.content.Intent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.annotation.VisibleForTesting
import android.widget.NumberPicker
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.entities.tags
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.service.SleepAlertScheduler
import com.example.productivityapp.util.SleepActionLogger
import com.example.productivityapp.viewmodel.SleepDaySummary
import com.example.productivityapp.viewmodel.SleepViewModel
import com.example.productivityapp.viewmodel.SleepViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val SleepBackdropLight = Color(0xFFF3FBF4)
private val SleepBackdropDark = Color(0xFF0B160F)
private val SleepSurfaceLight = Color(0xFFE3F4E6)
private val SleepSurfaceDark = Color(0xFF16261C)
private val SleepSurfaceAltLight = Color(0xFFF1FAF2)
private val SleepSurfaceAltDark = Color(0xFF1B2F23)
private val SleepTrackLight = Color(0xFFD2E6D5)
private val SleepTrackDark = Color(0xFF284033)
private val SleepAccentLight = Color(0xFF2F8F44)
private val SleepAccentDark = Color(0xFF8EDC9A)
private val SleepChipLight = Color(0xFFDDF5E1)
private val SleepChipDark = Color(0xFF1D3527)
private val SleepToneLight = Color(0xFF6E9C74)
private val SleepToneDark = Color(0xFFB4E0BB)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideSleepRepository(context)
    val userProfileRepo = RepositoryProvider.provideUserProfileRepository(context)
    val vm: SleepViewModel = viewModel(factory = SleepViewModelFactory(repo))

    val sessions = vm.sessions.collectAsState()
    val weeklySummary = vm.weeklySummary.collectAsState()
    val activeSession = vm.activeSession.collectAsState()
    val elapsedSeconds = vm.elapsedSeconds.collectAsState()
    val isPaused = vm.isPaused.collectAsState()
    val pendingReview = vm.pendingReviewSession.collectAsState()
    val pendingDetectedReview = vm.pendingDetectedReviewSession.collectAsState()
    val userProfile = userProfileRepo.observeUserProfile().collectAsState(initial = UserProfile())
    val canUseExactAlarm = remember(context) {
        SleepAlertScheduler.canScheduleExactAlarms(context.applicationContext)
    }
    SleepActionLogger.initialize(context.applicationContext)

    SleepScreenContent(
        sessions = sessions.value,
        weeklySummary = weeklySummary.value,
        activeSession = activeSession.value,
        elapsedSeconds = elapsedSeconds.value,
        isPaused = isPaused.value,
        pendingReviewSession = pendingReview.value,
        pendingDetectedReviewSession = pendingDetectedReview.value,
        sleepGoalMinutes = userProfile.value.nightlySleepGoalMinutes,
        typicalBedtimeMinutes = userProfile.value.typicalBedtimeMinutes,
        typicalWakeTimeMinutes = userProfile.value.typicalWakeTimeMinutes,
        canUseExactAlarm = canUseExactAlarm,
        onRequestExactAlarmAccess = {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
        onLogSleep = { startTimestamp, endTimestamp, quality, notes ->
            SleepActionLogger.logEvent(
                "sleep_button_click",
                mapOf("action" to "manual_sleep_save")
            )
            vm.logManualSleep(startTimestamp, endTimestamp, quality, notes)
        },
        onStartNapTimer = {
            if (activeSession.value?.detectionSource == SleepDetectionSource.NAP.storageValue) {
                SleepActionLogger.logEvent(
                    "sleep_button_click",
                    mapOf("action" to "stop_nap")
                )
                vm.stopSleep()
                SleepAlertScheduler.cancelNapReminder(context.applicationContext)
                if (SleepAlertScheduler.hasWakeAlarmScheduled(context.applicationContext)) {
                    SleepAlertScheduler.cancelWakeAlarm(context.applicationContext)
                }
            } else {
                SleepActionLogger.logEvent(
                    "sleep_button_click",
                    mapOf("action" to "start_nap")
                )
                SleepAlertScheduler.cancelNapReminder(context.applicationContext)
                if (SleepAlertScheduler.hasWakeAlarmScheduled(context.applicationContext)) {
                    SleepAlertScheduler.cancelWakeAlarm(context.applicationContext)
                }
                vm.startNapTimer()
                SleepAlertScheduler.scheduleNapReminder(context.applicationContext)
            }
        },
        onScheduleWakeAlarm = { triggerAtMillis, exact ->
            SleepAlertScheduler.scheduleWakeAlarm(context.applicationContext, triggerAtMillis, exact)
        },
        onPauseSleep = vm::pauseSleep,
        onResumeSleep = vm::resumeSleep,
        onStopSleep = {
            vm.stopSleep()
            SleepAlertScheduler.cancelNapReminder(context.applicationContext)
            SleepAlertScheduler.cancelWakeAlarm(context.applicationContext)
        },
        onSubmitReview = vm::submitSleepReview,
        onDismissReview = vm::dismissSleepReview,
        onAcceptDetectedReview = vm::acceptDetectedSleepReview,
        onAdjustDetectedReview = vm::adjustDetectedSleepReview,
        onMergeDetectedReview = vm::mergeDetectedSleepReviewWithPrevious,
        onDismissDetectedReview = vm::dismissDetectedSleepReview,
        onBack = onBack,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SleepScreenContent(
    sessions: List<SleepEntity>,
    weeklySummary: List<SleepDaySummary>,
    activeSession: SleepEntity?,
    elapsedSeconds: Long,
    isPaused: Boolean,
    pendingReviewSession: SleepEntity?,
    pendingDetectedReviewSession: SleepEntity?,
    sleepGoalMinutes: Int = 480,
    typicalBedtimeMinutes: Int = 22 * 60,
    typicalWakeTimeMinutes: Int = 7 * 60,
    canUseExactAlarm: Boolean = false,
    onRequestExactAlarmAccess: () -> Unit = {},
    onLogSleep: (Long, Long, Int?, String) -> Unit,
    onStartNapTimer: () -> Unit,
    onScheduleWakeAlarm: (Long, Boolean) -> Unit,
    onPauseSleep: () -> Unit,
    onResumeSleep: () -> Unit,
    onStopSleep: () -> Unit,
    onSubmitReview: (Int, String) -> Unit,
    onDismissReview: () -> Unit,
    onAcceptDetectedReview: () -> Unit,
    onAdjustDetectedReview: (Int, Int?, String) -> Unit,
    onMergeDetectedReview: () -> Unit,
    onDismissDetectedReview: () -> Unit,
    onBack: () -> Unit = {},
) {
    var quality by rememberSaveable(pendingReviewSession?.id) { mutableStateOf(4) }
    var notes by rememberSaveable(pendingReviewSession?.id) { mutableStateOf("") }
    var detectedQuality by rememberSaveable(pendingDetectedReviewSession?.id) { mutableStateOf(4) }
    var detectedNotes by rememberSaveable(pendingDetectedReviewSession?.id) { mutableStateOf("") }
    var detectedDurationMinutes by rememberSaveable(pendingDetectedReviewSession?.id) {
        mutableIntStateOf(
            ((pendingDetectedReviewSession?.durationSec ?: 8 * 3600L) / 60L).toInt().coerceAtLeast(1)
        )
    }
    var showWakeAlarmDialog by rememberSaveable { mutableStateOf(false) }
    var wakeAlarmExact by rememberSaveable { mutableStateOf(false) }
    var wakeAlarmTime by rememberSaveable { mutableStateOf("07:00") }
    var showManualSleepDialog by rememberSaveable { mutableStateOf(false) }
    var manualSleepStartDate by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(1).toString()) }
    var manualSleepEndDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var manualSleepStartHour by rememberSaveable { mutableIntStateOf(10) }
    var manualSleepStartMinute by rememberSaveable { mutableIntStateOf(0) }
    var manualSleepStartPeriod by rememberSaveable { mutableIntStateOf(1) }
    var manualSleepEndHour by rememberSaveable { mutableIntStateOf(6) }
    var manualSleepEndMinute by rememberSaveable { mutableIntStateOf(0) }
    var manualSleepEndPeriod by rememberSaveable { mutableIntStateOf(0) }
    var manualSleepQuality by rememberSaveable { mutableStateOf("") }
    var manualSleepNotes by rememberSaveable { mutableStateOf("") }
    var manualSleepError by rememberSaveable { mutableStateOf<String?>(null) }

    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val backdrop = if (darkTheme) SleepBackdropDark else SleepBackdropLight
    val surface = if (darkTheme) SleepSurfaceDark else SleepSurfaceLight
    val surfaceAlt = if (darkTheme) SleepSurfaceAltDark else SleepSurfaceAltLight
    val track = if (darkTheme) SleepTrackDark else SleepTrackLight
    val accent = if (darkTheme) SleepAccentDark else SleepAccentLight
    val chipColor = if (darkTheme) SleepChipDark else SleepChipLight
    val tone = if (darkTheme) SleepToneDark else SleepToneLight

    val todayDate = remember { java.time.LocalDate.now().toString() }
    val todaySessions = remember(sessions, todayDate) { sessions.filter { it.date == todayDate } }
    val completedSessions = remember(sessions) { sessions.filter { it.endTimestamp > 0L } }
    val todaySleepSeconds = remember(todaySessions, activeSession, elapsedSeconds) {
        val completed = todaySessions.sumOf { it.durationSec }
        completed + if (activeSession != null) elapsedSeconds else 0L
    }
    val todaySleepMinutes = (todaySleepSeconds / 60L).toInt()
    val goalMinutes = sleepGoalMinutes.coerceAtLeast(1)
    val progress = (todaySleepMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
    val goalLabel = formatDuration(goalMinutes * 60L)
    val sleepLabel = formatDuration(todaySleepSeconds)
    val statusText = when {
        pendingDetectedReviewSession != null -> "Detected automatically"
        pendingReviewSession != null -> "Awaiting manual review"
        activeSession != null -> if (isPaused) "Session paused" else "Sleep in progress"
        else -> "Ready to log sleep"
    }
    val statusDetail = when {
        pendingDetectedReviewSession != null -> "Provisional session needs confirmation, adjustment, merge, or dismiss."
        pendingReviewSession != null -> "Manual session needs a final quality review."
        activeSession != null -> if (isPaused) "The timer is paused." else "The session is running."
        else -> "Track tonight's sleep or review the latest history below."
    }

    val priorAverageMinutes = weeklySummary.dropLast(1).map { it.totalDurationSec / 60.0 }.takeIf { it.isNotEmpty() }?.average()
        ?: weeklySummary.map { it.totalDurationSec / 60.0 }.takeIf { it.isNotEmpty() }?.average()
        ?: 0.0
    val trendDeltaMinutes = (todaySleepMinutes - priorAverageMinutes).roundToInt()
    val trendText = when {
        priorAverageMinutes <= 0.0 && todaySleepMinutes > 0 -> "New night"
        abs(trendDeltaMinutes) < 15 -> "Stable"
        trendDeltaMinutes > 0 -> "+${trendDeltaMinutes}m vs avg"
        else -> "${abs(trendDeltaMinutes)}m below avg"
    }
    val trendColor = when {
        priorAverageMinutes <= 0.0 -> tone
        trendDeltaMinutes > 15 -> accent
        trendDeltaMinutes < -15 -> Color(0xFFE17B5A)
        else -> tone
    }

    val averageQuality = completedSessions.mapNotNull { it.sleepQuality }.takeIf { it.isNotEmpty() }?.average()
    val napSessions = completedSessions.filter { it.isNapLike() }
    val napMinutes = napSessions.sumOf { it.durationSec } / 60L
    val bedtimeDeviation = completedSessions.mapNotNull { it.startTimestamp.minuteOfDay() }
        .takeIf { it.isNotEmpty() }
        ?.map { circularMinuteDistance(it, typicalBedtimeMinutes) }
        ?.average()
    val wakeDeviation = completedSessions.mapNotNull { it.endTimestamp.minuteOfDay() }
        .takeIf { it.isNotEmpty() }
        ?.map { circularMinuteDistance(it, typicalWakeTimeMinutes) }
        ?.average()
    val sleepTip = buildSleepTip(
        activeSession = activeSession,
        pendingDetectedReviewSession = pendingDetectedReviewSession,
        averageQuality = averageQuality,
        bedtimeDeviationMinutes = bedtimeDeviation,
        wakeDeviationMinutes = wakeDeviation,
        napCount = napSessions.size,
    )
    var showSleepTip by rememberSaveable(sleepTip.title, sleepTip.message) { mutableStateOf(true) }
    val activeNapSession = activeSession?.detectionSource == SleepDetectionSource.NAP.storageValue
    val consistencyRows = listOf(
        SleepMetric(
            title = "Total sleep",
            value = sleepLabel,
            subtitle = "$todaySleepMinutes min of $goalMinutes min goal",
            accent = accent,
        ),
        SleepMetric(
            title = "Average quality",
            value = averageQuality?.let { "%.1f/5".format(it) } ?: "--",
            subtitle = if (averageQuality != null) "Based on completed sessions" else "No quality ratings yet",
            accent = tone,
        ),
        SleepMetric(
            title = "Bedtime consistency",
            value = bedtimeDeviation?.let { "±%dm".format(it.roundToInt()) } ?: "--",
            subtitle = if (bedtimeDeviation != null) "Target ${formatClockMinutes(typicalBedtimeMinutes)}" else "Add more nights to compare",
            accent = tone,
        ),
        SleepMetric(
            title = "Wake consistency",
            value = wakeDeviation?.let { "±%dm".format(it.roundToInt()) } ?: "--",
            subtitle = if (wakeDeviation != null) "Target ${formatClockMinutes(typicalWakeTimeMinutes)}" else "Add more wake times to compare",
            accent = tone,
        ),
        SleepMetric(
            title = "Naps",
            value = "${napSessions.size}",
            subtitle = if (napMinutes > 0) "${formatDuration(napMinutes * 60L)} total" else "No naps recorded",
            accent = tone,
        ),
        SleepMetric(
            title = "Trend",
            value = trendText,
            subtitle = if (priorAverageMinutes > 0.0) "Compared with the previous week" else "Once enough sessions exist, the trend will appear here",
            accent = trendColor,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
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
                .padding(innerPadding)
                .background(backdrop),
            color = backdrop,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 320.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        SleepActionCard(
                            onStartNapTimer = onStartNapTimer,
                            onOpenWakeAlarmDialog = { showWakeAlarmDialog = true },
                            onOpenManualSleepDialog = {
                                manualSleepError = null
                                showManualSleepDialog = true
                            },
                            activeNapSession = activeNapSession,
                            surface = surfaceAlt,
                            accent = accent,
                            tone = tone,
                        )
                    }

                    item {
                        SleepStateCard(
                            title = "Sleep insights",
                            subtitle = statusDetail,
                            label = statusText,
                            surface = surface,
                            accent = accent,
                            tone = tone,
                        )
                    }

                    if (showSleepTip) {
                        item {
                            SleepTipCard(
                                tip = sleepTip,
                                surface = surfaceAlt,
                                accent = accent,
                                tone = tone,
                                onDismiss = { showSleepTip = false },
                            )
                        }
                    }

                    items(consistencyRows.chunked(2)) { rowMetrics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowMetrics.forEach { metric ->
                                SleepMetricCard(
                                    metric = metric,
                                    surface = surfaceAlt,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowMetrics.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        SleepWeeklyChart(
                            weeklySummary = weeklySummary,
                            surface = surface,
                            accent = accent,
                            track = track,
                        )
                    }

                    if (pendingReviewSession != null) {
                        item {
                            SleepReviewCard(
                                quality = quality,
                                notes = notes,
                                surface = surface,
                                accent = accent,
                                onQualitySelected = { quality = it },
                                onNotesChanged = { notes = it },
                                onSave = { onSubmitReview(quality, notes) },
                                onDismiss = onDismissReview,
                            )
                        }
                    }

                    if (pendingDetectedReviewSession != null) {
                        item {
                            DetectedSleepReviewCard(
                                session = pendingDetectedReviewSession,
                                quality = detectedQuality,
                                notes = detectedNotes,
                                durationMinutes = detectedDurationMinutes,
                                surface = surface,
                                accent = accent,
                                onQualitySelected = { detectedQuality = it },
                                onNotesChanged = { detectedNotes = it },
                                onDurationChanged = { detectedDurationMinutes = it },
                                onAccept = onAcceptDetectedReview,
                                onAdjust = { onAdjustDetectedReview(detectedDurationMinutes, detectedQuality, detectedNotes) },
                                onMerge = onMergeDetectedReview,
                                onDismiss = onDismissDetectedReview,
                            )
                        }
                    }

                    item {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    items(sessions) { session ->
                        SleepHistoryCard(
                            session = session,
                            surface = surfaceAlt,
                            accent = accent,
                            tone = tone,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    SleepRing(
                        progress = progress,
                        totalLabel = sleepLabel,
                        goalLabel = goalLabel,
                        accent = accent,
                        track = track,
                        background = surface,
                    )
                }

                if (showWakeAlarmDialog) {
                    WakeAlarmDialog(
                        wakeAlarmTime = wakeAlarmTime,
                        exact = wakeAlarmExact,
                        exactAlarmAvailable = canUseExactAlarm,
                        onWakeAlarmTimeChanged = { wakeAlarmTime = it },
                        onExactChanged = { wakeAlarmExact = it },
                        onRequestExactAlarmAccess = onRequestExactAlarmAccess,
                        onDismiss = { showWakeAlarmDialog = false },
                        onSchedule = {
                            val triggerAtMillis = nextWakeTriggerMillis(wakeAlarmTime)
                            onScheduleWakeAlarm(triggerAtMillis, wakeAlarmExact)
                            showWakeAlarmDialog = false
                        },
                    )
                }

                if (showManualSleepDialog) {
                    ManualSleepDialog(
                        startDateText = manualSleepStartDate,
                        endDateText = manualSleepEndDate,
                        startHour = manualSleepStartHour,
                        startMinute = manualSleepStartMinute,
                        startPeriod = manualSleepStartPeriod,
                        endHour = manualSleepEndHour,
                        endMinute = manualSleepEndMinute,
                        endPeriod = manualSleepEndPeriod,
                        qualityText = manualSleepQuality,
                        notesText = manualSleepNotes,
                        errorText = manualSleepError,
                        onStartDateChanged = { manualSleepStartDate = it },
                        onEndDateChanged = { manualSleepEndDate = it },
                        onStartHourChanged = { manualSleepStartHour = it },
                        onStartMinuteChanged = { manualSleepStartMinute = it },
                        onStartPeriodChanged = { manualSleepStartPeriod = it },
                        onEndHourChanged = { manualSleepEndHour = it },
                        onEndMinuteChanged = { manualSleepEndMinute = it },
                        onEndPeriodChanged = { manualSleepEndPeriod = it },
                        onQualityChanged = { manualSleepQuality = it },
                        onNotesChanged = { manualSleepNotes = it },
                        onDismiss = { showManualSleepDialog = false },
                        onSave = {
                            val startDate = runCatching { LocalDate.parse(manualSleepStartDate) }.getOrNull()
                            val endDate = runCatching { LocalDate.parse(manualSleepEndDate) }.getOrNull()
                            if (startDate == null || endDate == null) {
                                manualSleepError = "Pick valid start and end dates."
                                return@ManualSleepDialog
                            }

                            val zone = ZoneId.systemDefault()
                            val start = startDate.atTime(manualSleepStartHour.toTwentyFourHour(manualSleepStartPeriod), manualSleepStartMinute).atZone(zone)
                            val end = endDate.atTime(manualSleepEndHour.toTwentyFourHour(manualSleepEndPeriod), manualSleepEndMinute).atZone(zone)
                            if (!end.isAfter(start)) {
                                manualSleepError = "End date and time must be after the start."
                                return@ManualSleepDialog
                            }

                            val quality = manualSleepQuality.trim().toIntOrNull()?.takeIf { it in 1..5 }
                            val notes = manualSleepNotes.trim()
                            onLogSleep(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), quality, notes)
                            manualSleepError = null
                            showManualSleepDialog = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepActionCard(
    onStartNapTimer: () -> Unit,
    onOpenWakeAlarmDialog: () -> Unit,
    onOpenManualSleepDialog: () -> Unit,
    activeNapSession: Boolean,
    surface: Color,
    accent: Color,
    tone: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                if (activeNapSession) "Stop the active nap, set an alarm, or log sleep manually."
                else "Start a nap, set an alarm, or log sleep manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = tone,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onStartNapTimer,
                    modifier = Modifier.semantics { contentDescription = if (activeNapSession) "Stop nap" else "Start nap" },
                ) {
                    Text(if (activeNapSession) "Stop Nap" else "Start Nap")
                }
                OutlinedButton(
                    onClick = onOpenWakeAlarmDialog,
                    modifier = Modifier.semantics { contentDescription = "Alarm" },
                ) {
                    Text("Alarm")
                }
                OutlinedButton(
                    onClick = onOpenManualSleepDialog,
                    modifier = Modifier.semantics { contentDescription = "Log sleep" },
                ) {
                    Text("Log Sleep")
                }
            }
        }
    }
}

@Composable
private fun WakeAlarmDialog(
    wakeAlarmTime: String,
    exact: Boolean,
    exactAlarmAvailable: Boolean,
    onWakeAlarmTimeChanged: (String) -> Unit,
    onExactChanged: (Boolean) -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onDismiss: () -> Unit,
    onSchedule: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set wake-up alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose when to wake up and whether to request the more precise alarm path.")
                OutlinedTextField(
                    value = wakeAlarmTime,
                    onValueChange = { onWakeAlarmTimeChanged(it.filter { ch -> ch.isDigit() || ch == ':' }.take(5)) },
                    label = { Text("Wake time (HH:mm)") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = exact,
                        onCheckedChange = onExactChanged,
                        modifier = Modifier.semantics { contentDescription = "Use exact alarm switch" },
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Use exact alarm")
                        Text("Falls back to a timed reminder if exact alarms are unavailable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (exact && !exactAlarmAvailable) {
                    Text(
                        "Exact alarms are not currently available. You can open system settings to grant access, or keep the timed fallback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRequestExactAlarmAccess) {
                        Text("Open exact alarm settings")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchedule) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualSleepDialog(
    startDateText: String,
    endDateText: String,
    startHour: Int,
    startMinute: Int,
    startPeriod: Int,
    endHour: Int,
    endMinute: Int,
    endPeriod: Int,
    qualityText: String,
    notesText: String,
    errorText: String?,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onStartHourChanged: (Int) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onStartPeriodChanged: (Int) -> Unit,
    onEndHourChanged: (Int) -> Unit,
    onEndMinuteChanged: (Int) -> Unit,
    onEndPeriodChanged: (Int) -> Unit,
    onQualityChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dialogContainerColor = if (isDark) SleepSurfaceDark else SleepSurfaceLight
    val dialogContentColor = if (isDark) SleepToneDark else SleepToneLight
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainerColor,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        title = { Text("Log sleep manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pick the start and end dates, then set the times with the wheel picker.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectionButton(
                        label = "Start date",
                        value = formatSleepDateLabel(startDateText),
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    SelectionButton(
                        label = "Start time",
                        value = formatSleepTimeLabel(startHour, startMinute, startPeriod),
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectionButton(
                        label = "End date",
                        value = formatSleepDateLabel(endDateText),
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    SelectionButton(
                        label = "End time",
                        value = formatSleepTimeLabel(endHour, endMinute, endPeriod),
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = qualityText,
                    onValueChange = { onQualityChanged(it.filter(Char::isDigit).take(1)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quality (optional 1-5)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes") },
                    minLines = 2,
                )
                if (errorText != null) {
                    Text(errorText, color = Color(0xFFE17B5A), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor)) {
                Text("Cancel")
            }
        },
    )

    if (showStartDatePicker) {
        DateSelectorDialog(
            title = "Select start date",
            initialDateText = startDateText,
            onDismiss = { showStartDatePicker = false },
            onConfirm = {
                onStartDateChanged(it)
                showStartDatePicker = false
            },
        )
    }
    if (showEndDatePicker) {
        DateSelectorDialog(
            title = "Select end date",
            initialDateText = endDateText,
            onDismiss = { showEndDatePicker = false },
            onConfirm = {
                onEndDateChanged(it)
                showEndDatePicker = false
            },
        )
    }
    if (showStartTimePicker) {
        ScrollableTimePickerDialog(
            title = "Select start time",
            hour = startHour,
            minute = startMinute,
            period = startPeriod,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute, amPm ->
                onStartHourChanged(hour)
                onStartMinuteChanged(minute)
                onStartPeriodChanged(amPm)
                showStartTimePicker = false
            },
        )
    }
    if (showEndTimePicker) {
        ScrollableTimePickerDialog(
            title = "Select end time",
            hour = endHour,
            minute = endMinute,
            period = endPeriod,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute, amPm ->
                onEndHourChanged(hour)
                onEndMinuteChanged(minute)
                onEndPeriodChanged(amPm)
                showEndTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorDialog(
    title: String,
    initialDateText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialDateMillis = remember(initialDateText) {
        runCatching {
            LocalDate.parse(initialDateText).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = datePickerState.selectedDateMillis ?: return@TextButton
                onConfirm(
                    java.time.Instant.ofEpochMilli(selected).atZone(ZoneId.systemDefault()).toLocalDate()
                        .toString()
                )
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun ScrollableTimePickerDialog(
    title: String,
    hour: Int,
    minute: Int,
    period: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit,
) {
    var selectedHour by remember(hour) { mutableIntStateOf(hour.coerceIn(1, 12)) }
    var selectedMinute by remember(minute) { mutableIntStateOf(minute.coerceIn(0, 59)) }
    var selectedPeriod by remember(period) { mutableIntStateOf(period.coerceIn(0, 1)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SpinnerColumn(
                    label = "Hour",
                    values = (1..12).map(Int::toString),
                    selectedIndex = selectedHour - 1,
                    modifier = Modifier.weight(1f),
                    onSelected = { selectedHour = it + 1 },
                )
                SpinnerColumn(
                    label = "Minute",
                    values = (0..59).map { "%02d".format(it) },
                    selectedIndex = selectedMinute,
                    modifier = Modifier.weight(1f),
                    onSelected = { selectedMinute = it },
                )
                SpinnerColumn(
                    label = "AM/PM",
                    values = listOf("AM", "PM"),
                    selectedIndex = selectedPeriod,
                    modifier = Modifier.weight(1f),
                    onSelected = { selectedPeriod = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute, selectedPeriod) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SpinnerColumn(
    label: String,
    values: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        AndroidView(
            modifier = Modifier.height(160.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 0
                    maxValue = values.lastIndex
                    displayedValues = values.toTypedArray()
                    wrapSelectorWheel = true
                    value = selectedIndex.coerceIn(0, values.lastIndex)
                    setOnValueChangedListener { _, _, newVal -> onSelected(newVal) }
                }
            },
            update = { picker ->
                if (picker.maxValue != values.lastIndex) {
                    picker.minValue = 0
                    picker.maxValue = values.lastIndex
                    picker.displayedValues = values.toTypedArray()
                }
                val safeIndex = selectedIndex.coerceIn(0, values.lastIndex)
                if (picker.value != safeIndex) {
                    picker.value = safeIndex
                }
            },
        )
    }
}

@Composable
private fun SelectionButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatSleepDateLabel(dateText: String): String {
    return runCatching {
        LocalDate.parse(dateText).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }.getOrElse { dateText }
}

private fun formatSleepTimeLabel(hour: Int, minute: Int, period: Int): String {
    val time = LocalTime.of(hour.toTwentyFourHour(period), minute.coerceIn(0, 59))
    return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}

private fun Int.toTwentyFourHour(period: Int): Int {
    val normalizedHour = coerceIn(1, 12) % 12
    return if (period == 1) normalizedHour + 12 else normalizedHour
}

@Composable
private fun SleepRing(
    progress: Float,
    totalLabel: String,
    goalLabel: String,
    accent: Color,
    track: Color,
    background: Color,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(260.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        background,
                        if (isDark) background.copy(alpha = 0.9f) else Color.White,
                    ),
                )
            )
            .padding(14.dp),
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
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 28f, cap = StrokeCap.Round),
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
                "${(progress * 100).roundToInt()}% complete",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
    }
}

@Composable
private fun SleepTrendChip(
    text: String,
    surface: Color,
    accent: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SleepStateCard(
    title: String,
    subtitle: String,
    label: String,
    surface: Color,
    accent: Color,
    tone: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = tone)
        }
    }
}

@Composable
private fun SleepTipCard(
    tip: SleepTip,
    surface: Color,
    accent: Color,
    tone: Color,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sleep tip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(tip.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = "Dismiss sleep tip" },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = tone,
                    )
                }
            }
            Text(tip.message, style = MaterialTheme.typography.bodyMedium, color = tone)
        }
    }
}

@Composable
private fun SleepMetricCard(
    metric: SleepMetric,
    surface: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(metric.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(metric.accent)
                )
            }
            Text(metric.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(metric.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SleepWeeklyChart(
    weeklySummary: List<SleepDaySummary>,
    surface: Color,
    accent: Color,
    track: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Last 7 nights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Duration by night", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
            }

            val maxDuration = max(weeklySummary.maxOfOrNull { it.totalDurationSec } ?: 1L, 1L).toFloat()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                weeklySummary.forEach { day ->
                    val ratio = (day.totalDurationSec.toFloat() / maxDuration).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(formatHours(day.totalDurationSec), style = MaterialTheme.typography.labelSmall)
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((120 * ratio).coerceAtLeast(8f).dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(accent, track),
                                    ),
                                )
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
    surface: Color,
    accent: Color,
    onQualitySelected: (Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Manual review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Finalize the session details before it is treated as confirmed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = quality == rating,
                            onClick = { onQualitySelected(rating) },
                            modifier = Modifier.semantics { contentDescription = "Select quality $rating" }
                        )
                        Text("$rating / 5")
                    }
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
                Button(onClick = onSave, modifier = Modifier.semantics { contentDescription = "Save sleep review" }) {
                    Text("Save review")
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "Skip sleep review" }) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun DetectedSleepReviewCard(
    session: SleepEntity,
    quality: Int,
    notes: String,
    durationMinutes: Int,
    surface: Color,
    accent: Color,
    onQualitySelected: (Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDurationChanged: (Int) -> Unit,
    onAccept: () -> Unit,
    onAdjust: () -> Unit,
    onMerge: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Text("Detected sleep session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Confidence ${session.confidenceScore.times(100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Status: ${session.reviewState.toReadableLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept) { Text("Confirm") }
                OutlinedButton(onClick = onMerge) { Text("Merge") }
                OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
            }
            OutlinedTextField(
                value = durationMinutes.toString(),
                onValueChange = { text -> onDurationChanged(text.filter(Char::isDigit).toIntOrNull() ?: durationMinutes) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Duration (minutes)") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = quality == rating,
                            onClick = { onQualitySelected(rating) },
                            modifier = Modifier.semantics { contentDescription = "Select detected quality $rating" }
                        )
                        Text("$rating")
                    }
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 2,
            )
            Button(onClick = onAdjust, modifier = Modifier.fillMaxWidth()) {
                Text("Save adjustments")
            }
            Text(
                "If the inferred window is off, adjust the entry before confirming.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SleepHistoryCard(
    session: SleepEntity,
    surface: Color,
    accent: Color,
    tone: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(session.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
            }
            Text("${formatTimestamp(session.startTimestamp)} → ${formatTimestamp(session.endTimestamp)}")
            Text("Duration: ${formatDuration(session.durationSec)}")
            Text("Quality: ${session.sleepQuality ?: "--"}/5")
            Text("Source: ${session.detectionSource.toReadableLabel()} · ${session.reviewState.toReadableLabel()}", color = tone)
            if (session.tags.isNotEmpty()) {
                Text("Tags: ${session.tags.joinToString(", ")}", color = tone)
            }
            if (!session.notes.isNullOrBlank()) {
                Text("Notes: ${session.notes}")
            }
        }
    }
}

private data class SleepMetric(
    val title: String,
    val value: String,
    val subtitle: String,
    val accent: Color,
)

private fun SleepEntity.isNapLike(): Boolean {
    return detectionSource == SleepDetectionSource.NAP.storageValue ||
        tags.any { it.equals("nap", ignoreCase = true) }
}

private fun Long.minuteOfDay(): Int? {
    if (this <= 0L) return null
    val zoned = java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault())
    return zoned.hour * 60 + zoned.minute
}

private fun circularMinuteDistance(first: Int, second: Int): Int {
    val direct = abs(first - second)
    return minOf(direct, 1440 - direct)
}

private fun String.toReadableLabel(): String {
    return replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.titlecase() }
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

private fun formatClockMinutes(totalMinutes: Int): String {
    val hours = (totalMinutes / 60).coerceIn(0, 23)
    val minutes = (totalMinutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hours, minutes)
}

@VisibleForTesting
internal fun nextWakeTriggerMillis(
    timeText: String,
    now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(),
): Long {
    val minutes = timeText.parseClockMinutesOrNull() ?: (7 * 60)
    val today = now.toLocalDate().atTime(minutes / 60, minutes % 60).atZone(now.zone)
    val trigger = if (today.isAfter(now)) today else today.plusDays(1)
    return trigger.toInstant().toEpochMilli()
}

private fun String.parseClockMinutesOrNull(): Int? {
    val parts = trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}
