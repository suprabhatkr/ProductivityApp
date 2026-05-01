package com.example.productivityapp.ui.mindfulness

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import com.example.productivityapp.data.entities.type
import com.example.productivityapp.data.model.MindfulnessSessionType
import com.example.productivityapp.viewmodel.MindfulnessViewModel
import com.example.productivityapp.viewmodel.MindfulnessViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val MindBackdropLight = Color(0xFFF5FBFA)
private val MindBackdropDark = Color(0xFF0E161A)
private val MindSurfaceLight = Color(0xFFFFFFFF)
private val MindSurfaceDark = Color(0xFF162127)
private val MindSurfaceAltLight = Color(0xFFEAF7F4)
private val MindSurfaceAltDark = Color(0xFF1D2B31)
private val MindAccentLight = Color(0xFF0F8C83)
private val MindAccentDark = Color(0xFF86E2D8)
private val MindToneLight = Color(0xFF10615D)
private val MindToneDark = Color(0xFFD4F7F2)
private val MindChipLight = Color(0xFFD9F2EE)
private val MindChipDark = Color(0xFF244039)
private val MindWarmLight = Color(0xFF8E6BFF)
private val MindWarmDark = Color(0xFFCFC0FF)
private val MindLogLight = Color(0xFFFFF7EB)
private val MindLogDark = Color(0xFF2B241C)

private val MindfulnessDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a")

@Composable
fun MindfulnessScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = RepositoryProvider.provideMindfulnessRepository(context)
    val viewModel: MindfulnessViewModel = viewModel(factory = MindfulnessViewModelFactory(repository))
    val sessions by viewModel.sessions.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val selectedSessionType by viewModel.selectedSessionType.collectAsState()
    val reflectionDraft by viewModel.reflectionDraft.collectAsState()

    MindfulnessScreenContent(
        sessions = sessions,
        logs = logs,
        activeSession = activeSession,
        selectedSessionType = selectedSessionType,
        reflectionDraft = reflectionDraft,
        onSelectSessionType = viewModel::selectSessionType,
        onUpdateReflectionDraft = viewModel::updateReflectionDraft,
        onStartSession = viewModel::startSession,
        onEndSession = viewModel::endSession,
        onSaveReflection = viewModel::saveReflection,
        onBack = onBack,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun MindfulnessScreenContent(
    sessions: List<MindfulnessSessionEntity>,
    logs: List<MindLogEntity>,
    activeSession: MindfulnessSessionEntity?,
    selectedSessionType: MindfulnessSessionType,
    reflectionDraft: String,
    onSelectSessionType: (MindfulnessSessionType) -> Unit,
    onUpdateReflectionDraft: (String) -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    onSaveReflection: () -> Unit,
    onBack: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backdrop = if (isDark) MindBackdropDark else MindBackdropLight
    val surface = if (isDark) MindSurfaceDark else MindSurfaceLight
    val surfaceAlt = if (isDark) MindSurfaceAltDark else MindSurfaceAltLight
    val accent = if (isDark) MindAccentDark else MindAccentLight
    val tone = if (isDark) MindToneDark else MindToneLight
    val chip = if (isDark) MindChipDark else MindChipLight
    val warm = if (isDark) MindWarmDark else MindWarmLight
    val logSurface = if (isDark) MindLogDark else MindLogLight
    val currentTimeMs by produceState(initialValue = System.currentTimeMillis(), key1 = activeSession?.id) {
        value = System.currentTimeMillis()
        if (activeSession != null) {
            while (true) {
                delay(1_000L)
                value = System.currentTimeMillis()
            }
        }
    }
    val snapshot = buildMindfulnessSnapshot(
        sessions = sessions,
        logs = logs,
        activeSession = activeSession,
        currentTimeMs = currentTimeMs,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mindfulness", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
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
                    MindfulnessHeroCard(
                        snapshot = snapshot,
                        selectedSessionType = selectedSessionType,
                        surface = surface,
                        chipColor = chip,
                        accent = accent,
                        tone = tone,
                    )
                }

                item {
                    MindfulnessPracticeCard(
                        selectedSessionType = selectedSessionType,
                        activeSession = activeSession,
                        currentTimeMs = currentTimeMs,
                        surface = surfaceAlt,
                        accent = accent,
                        warm = warm,
                        onSelectSessionType = onSelectSessionType,
                        onStartSession = onStartSession,
                        onEndSession = onEndSession,
                    )
                }

                item {
                    Text(
                        "Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        snapshot.metrics.chunked(2).first().forEach { metric ->
                            MindfulnessMetricCard(
                                title = metric.title,
                                value = metric.value,
                                subtitle = metric.subtitle,
                                accent = accent,
                                surface = surface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        snapshot.metrics.chunked(2).getOrNull(1)?.forEach { metric ->
                            MindfulnessMetricCard(
                                title = metric.title,
                                value = metric.value,
                                subtitle = metric.subtitle,
                                accent = accent,
                                surface = surface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item {
                    MindfulnessReflectionCard(
                        reflectionDraft = reflectionDraft,
                        surface = logSurface,
                        accent = accent,
                        onUpdateReflectionDraft = onUpdateReflectionDraft,
                        onSaveReflection = onSaveReflection,
                    )
                }

                item {
                    Text(
                        "Recent practice",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (sessions.isEmpty()) {
                    item {
                        EmptyMindfulnessCard(
                            text = "Your breathing and meditation sessions will appear here once you begin.",
                            surface = surface,
                        )
                    }
                } else {
                    items(sessions.take(6), key = { it.id }) { session ->
                        MindfulnessSessionHistoryCard(
                            session = session,
                            currentTimeMs = currentTimeMs,
                            surface = surface,
                            accent = accent,
                            warm = warm,
                        )
                    }
                }

                item {
                    Text(
                        "Recent reflections",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (logs.isEmpty()) {
                    item {
                        EmptyMindfulnessCard(
                            text = "Log what is on your mind whenever you need a calm outlet.",
                            surface = surface,
                        )
                    }
                } else {
                    items(logs.take(6), key = { it.id }) { log ->
                        MindLogHistoryCard(
                            log = log,
                            surface = surface,
                            accent = accent,
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
private fun MindfulnessHeroCard(
    snapshot: MindfulnessSnapshot,
    selectedSessionType: MindfulnessSessionType,
    surface: Color,
    chipColor: Color,
    accent: Color,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        snapshot.headline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        snapshot.supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(chipColor, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForMindfulnessType(snapshot.activeType ?: selectedSessionType),
                        contentDescription = null,
                        tint = accent,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MindfulnessChip(
                    text = snapshot.chipOne,
                    background = chipColor,
                    color = tone,
                )
                MindfulnessChip(
                    text = snapshot.chipTwo,
                    background = chipColor,
                    color = tone,
                )
            }
        }
    }
}

@Composable
private fun MindfulnessPracticeCard(
    selectedSessionType: MindfulnessSessionType,
    activeSession: MindfulnessSessionEntity?,
    currentTimeMs: Long,
    surface: Color,
    accent: Color,
    warm: Color,
    onSelectSessionType: (MindfulnessSessionType) -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
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
                "Practice space",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MindfulnessModeCard(
                    type = MindfulnessSessionType.BREATHING,
                    selectedType = selectedSessionType,
                    enabled = activeSession == null,
                    accent = accent,
                    surface = Color.Transparent,
                    onClick = onSelectSessionType,
                    modifier = Modifier.weight(1f),
                )
                MindfulnessModeCard(
                    type = MindfulnessSessionType.MEDITATION,
                    selectedType = selectedSessionType,
                    enabled = activeSession == null,
                    accent = warm,
                    surface = Color.Transparent,
                    onClick = onSelectSessionType,
                    modifier = Modifier.weight(1f),
                )
            }

            if (activeSession == null) {
                Text(
                    "Begin a short breathing or meditation session and end it when you feel settled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onStartSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Start ${selectedSessionType.label}" },
                ) {
                    Text("Start ${selectedSessionType.label}")
                }
            } else {
                val elapsedSec = ((currentTimeMs - activeSession.startTime) / 1000L).coerceAtLeast(0L)
                Text(
                    activeSession.type.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    formatMindfulnessDuration(elapsedSec),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Started ${formatMindfulnessDate(activeSession.startTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onEndSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "End mindfulness session" },
                ) {
                    Text("End session")
                }
            }
        }
    }
}

@Composable
private fun MindfulnessModeCard(
    type: MindfulnessSessionType,
    selectedType: MindfulnessSessionType,
    enabled: Boolean,
    accent: Color,
    surface: Color,
    onClick: (MindfulnessSessionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = type == selectedType
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.14f) else surface,
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.clickable(enabled = enabled) { onClick(type) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = if (selected) 0.2f else 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForMindfulnessType(type),
                    contentDescription = null,
                    tint = accent,
                )
            }
            Text(
                type.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (type == MindfulnessSessionType.BREATHING) {
                    "Slow down and reset with your breath."
                } else {
                    "Sit still and give your mind some quiet."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MindfulnessMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    surface: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = accent)
        }
    }
}

@Composable
private fun MindfulnessReflectionCard(
    reflectionDraft: String,
    surface: Color,
    accent: Color,
    onUpdateReflectionDraft: (String) -> Unit,
    onSaveReflection: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = accent)
                Text(
                    "Log your mind",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                "Write down what you are noticing, feeling, or trying to let go of.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = reflectionDraft,
                onValueChange = onUpdateReflectionDraft,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                placeholder = { Text("What's on your mind right now?") },
            )
            Button(
                onClick = onSaveReflection,
                enabled = reflectionDraft.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Save reflection" },
            ) {
                Text("Save reflection")
            }
        }
    }
}

@Composable
private fun MindfulnessSessionHistoryCard(
    session: MindfulnessSessionEntity,
    currentTimeMs: Long,
    surface: Color,
    accent: Color,
    warm: Color,
) {
    val durationSec = session.endTime?.let { session.durationSec }
        ?: ((currentTimeMs - session.startTime) / 1000L).coerceAtLeast(0L)
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
                        .background(
                            color = (if (session.type == MindfulnessSessionType.BREATHING) accent else warm).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForMindfulnessType(session.type),
                        contentDescription = null,
                        tint = if (session.type == MindfulnessSessionType.BREATHING) accent else warm,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        session.type.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        formatMindfulnessDate(session.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatMindfulnessDuration(durationSec),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (session.endTime == null) "Live" else "Complete",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (session.endTime == null) accent else warm,
                )
            }
        }
    }
}

@Composable
private fun MindLogHistoryCard(
    log: MindLogEntity,
    surface: Color,
    accent: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                formatMindfulnessDate(log.createdAt),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
            Text(
                log.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyMindfulnessCard(
    text: String,
    surface: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun MindfulnessChip(
    text: String,
    background: Color,
    color: Color,
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

private data class MindfulnessSnapshot(
    val headline: String,
    val supporting: String,
    val chipOne: String,
    val chipTwo: String,
    val activeType: MindfulnessSessionType?,
    val metrics: List<MindfulnessMetric>,
)

private data class MindfulnessMetric(
    val title: String,
    val value: String,
    val subtitle: String,
)

private fun buildMindfulnessSnapshot(
    sessions: List<MindfulnessSessionEntity>,
    logs: List<MindLogEntity>,
    activeSession: MindfulnessSessionEntity?,
    currentTimeMs: Long,
): MindfulnessSnapshot {
    val today = LocalDate.now()
    val todaySessions = sessions.filter { toLocalDate(it.startTime) == today }
    val todayLogs = logs.filter { toLocalDate(it.createdAt) == today }
    val todaySeconds = todaySessions.sumOf { session ->
        session.endTime?.let { session.durationSec } ?: 0L
    }
    val weekSeconds = sessions
        .filter { toLocalDate(it.startTime).isAfter(today.minusDays(7)) || toLocalDate(it.startTime) == today.minusDays(7) }
        .sumOf { session -> session.endTime?.let { session.durationSec } ?: 0L }
    val latestSession = sessions.firstOrNull()
    val latestLog = logs.firstOrNull()

    return MindfulnessSnapshot(
        headline = when {
            activeSession != null -> "${activeSession.type.label} in progress"
            todaySeconds > 0L -> "${formatMindfulnessDuration(todaySeconds)} of calm today"
            todayLogs.isNotEmpty() -> "${todayLogs.size} reflection${if (todayLogs.size == 1) "" else "s"} today"
            else -> "A calmer moment starts here"
        },
        supporting = when {
            activeSession != null -> "Stay present. End the session whenever you feel settled."
            latestSession != null -> "Last practice: ${latestSession.type.label} at ${formatMindfulnessTime(latestSession.startTime)}"
            latestLog != null -> "Your latest reflection was saved ${formatMindfulnessTime(latestLog.createdAt)}"
            else -> "Breathe, meditate, and journal your thoughts in one peaceful space."
        },
        chipOne = if (todaySeconds > 0L) "Today ${formatMindfulnessDuration(todaySeconds)}" else "Today 0m",
        chipTwo = if (todayLogs.isNotEmpty()) "${todayLogs.size} mind log${if (todayLogs.size == 1) "" else "s"}" else "Breathing + meditation",
        activeType = activeSession?.type,
        metrics = listOf(
            MindfulnessMetric(
                title = "Today",
                value = formatMindfulnessDuration(todaySeconds),
                subtitle = "${todaySessions.size} session${if (todaySessions.size == 1) "" else "s"}",
            ),
            MindfulnessMetric(
                title = "Reflections",
                value = todayLogs.size.toString(),
                subtitle = "Thoughts saved today",
            ),
            MindfulnessMetric(
                title = "This week",
                value = formatMindfulnessDuration(weekSeconds),
                subtitle = "Across the last 7 days",
            ),
            MindfulnessMetric(
                title = "Latest",
                value = latestSession?.type?.label ?: "--",
                subtitle = latestLog?.let { "Last log ${formatMindfulnessTime(it.createdAt)}" } ?: "Start when ready",
            ),
        ),
    )
}

private fun iconForMindfulnessType(type: MindfulnessSessionType): ImageVector {
    return when (type) {
        MindfulnessSessionType.BREATHING -> Icons.Filled.Spa
        MindfulnessSessionType.MEDITATION -> Icons.Filled.SelfImprovement
    }
}

private fun formatMindfulnessDate(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(MindfulnessDateFormatter)
}

private fun formatMindfulnessTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
}

private fun toLocalDate(timestamp: Long): LocalDate {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}

internal fun formatMindfulnessDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0L) {
        "%dh %02dm".format(hours, minutes % 60L)
    } else if (minutes > 0L) {
        "%dm".format(minutes)
    } else {
        "%ds".format(seconds)
    }
}
