package com.example.productivityapp.ui.run

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import com.example.productivityapp.data.entities.type
import com.example.productivityapp.run.RunAnalyticsCalculator
import com.example.productivityapp.run.RunAnalysisSnapshot
import com.example.productivityapp.run.RunReplayExportConfig
import com.example.productivityapp.run.RunReplayExportException
import com.example.productivityapp.run.RunReplayExportResult
import com.example.productivityapp.run.RunReplayHelper
import com.example.productivityapp.run.RunReplayTimelineBuilder
import com.example.productivityapp.run.RunReplayFrame
import com.example.productivityapp.run.RunSpeedSample
import com.example.productivityapp.run.RunSplitSummary
import com.example.productivityapp.viewmodel.RunViewModel
import com.example.productivityapp.viewmodel.RunViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    runId: Long?,
    onBack: () -> Unit = {},
) {
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    val repo = RepositoryProvider.provideRunRepository(currentContext)
    val uiStateStore = RepositoryProvider.provideUiStateStore(currentContext)
    val vm: RunViewModel = viewModel(factory = RunViewModelFactory(repo, uiStateStore))
    val run = remember(vm, runId) {
        runId?.let(vm::observeRun) ?: flowOf<RunEntity?>(null)
    }.collectAsState(initial = null)
    val runPoints = remember(vm, runId) {
        runId?.let(vm::observeRunPoints) ?: flowOf<List<RunPointEntity>>(emptyList())
    }.collectAsState(initial = emptyList())

    RunDetailsScreenContent(
        run = run.value,
        runPoints = runPoints.value,
        onBack = onBack,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreenContent(
    run: RunEntity?,
    runPoints: List<RunPointEntity> = emptyList(),
    onBack: () -> Unit = {},
) {
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    val palette = rememberRunPalette()

    val replayPoints = remember(run?.id, run?.polyline, runPoints) {
        if (runPoints.isNotEmpty()) {
            RunReplayHelper.fromPointEntities(runPoints)
        } else {
            RunReplayHelper.decodeEncodedPolyline(run?.polyline.orEmpty())
        }
    }
    val analyticsCalculator = remember { RunAnalyticsCalculator() }
    val replayTimelineBuilder = remember { RunReplayTimelineBuilder() }
    val replayExporter = remember(currentContext) { RepositoryProvider.provideRunReplayExporter(currentContext) }
    val coroutineScope = rememberCoroutineScope()
    val analyticsSnapshot = remember(run, runPoints) {
        run?.let { analyticsCalculator.buildSnapshot(it, runPoints) }
    }
    val replayFrames = remember(run?.id, replayPoints) {
        replayTimelineBuilder.buildInteractiveTimeline(replayPoints)
    }
    var replayFrameIndex by rememberSaveable(run?.id) { mutableStateOf(0) }
    var replayPlaying by rememberSaveable(run?.id) { mutableStateOf(false) }
    var exportUiState by remember(run?.id) { mutableStateOf<ReplayExportUiState>(ReplayExportUiState.Idle) }
    val currentReplayFrame = replayFrames.getOrNull(replayFrameIndex)
        ?: replayFrames.lastOrNull()

    LaunchedEffect(replayPlaying, replayFrames.size, replayFrameIndex) {
        if (!replayPlaying || replayFrames.size < 2) return@LaunchedEffect
        while (replayPlaying) {
            delay((currentReplayFrame?.frameDurationMs ?: 0L).coerceAtLeast(60L))
            if (replayFrameIndex >= replayFrames.lastIndex) {
                replayPlaying = false
            } else {
                replayFrameIndex += 1
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${run?.type?.label ?: "Outdoor"} details",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = palette.backdrop,
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = palette.backdrop,
        ) {
            if (run == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceAlt),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Session not found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "The selected outdoor session could not be loaded. Go back to the dashboard and pick another session.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(onClick = onBack) { Text("Back to Run & Walk") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        RunDetailsHeaderCard(
                            run = run,
                            surface = palette.surface,
                            tone = palette.tone,
                            chipColor = palette.chip,
                        )
                    }

                    item {
                        RunDetailsRouteCard(
                            run = run,
                            replayFrames = replayFrames,
                            replayFrameIndex = replayFrameIndex,
                            currentReplayFrame = currentReplayFrame,
                            replayPlaying = replayPlaying,
                            surface = palette.surfaceAlt,
                            accent = palette.accent,
                            onReplayFrameIndexChange = {
                                replayPlaying = false
                                replayFrameIndex = it.coerceIn(0, max(replayFrames.lastIndex, 0))
                            },
                            onReplayReset = {
                                replayPlaying = false
                                replayFrameIndex = 0
                            },
                            onReplayToggle = {
                                if (replayFrames.size < 2) return@RunDetailsRouteCard
                                if (replayFrameIndex >= replayFrames.lastIndex) replayFrameIndex = 0
                                replayPlaying = !replayPlaying
                            },
                            exportUiState = exportUiState,
                            onExportReplay = {
                                if (run.id <= 0L) return@RunDetailsRouteCard
                                // If an exported result already exists, open the share intent instead of re-encoding
                                if (exportUiState is ReplayExportUiState.Ready) {
                                    val result = (exportUiState as ReplayExportUiState.Ready).result
                                    try {
                                        launchShareIntent(currentContext, result)
                                    } catch (_: ActivityNotFoundException) {
                                        exportUiState = ReplayExportUiState.Error("No compatible app is available to share this replay video.")
                                    } catch (_: IllegalArgumentException) {
                                        exportUiState = ReplayExportUiState.Error("Replay sharing could not be started.")
                                    }
                                    return@RunDetailsRouteCard
                                }

                                replayPlaying = false
                                exportUiState = ReplayExportUiState.Exporting
                                coroutineScope.launch {
                                    exportUiState = try {
                                        val result = replayExporter.exportRunReplay(
                                            runId = run.id,
                                            isDarkTheme = palette.useDarkPalette,
                                            analyticsSnapshot = analyticsSnapshot,
                                            config = RunReplayExportConfig(),
                                        )
                                        launchShareIntent(currentContext, result)
                                        ReplayExportUiState.Ready(result)
                                    } catch (error: RunReplayExportException) {
                                        ReplayExportUiState.Error(error.message ?: "Replay export failed.")
                                    } catch (error: ActivityNotFoundException) {
                                        ReplayExportUiState.Error("No compatible app is available to share this replay video.")
                                    } catch (error: IllegalArgumentException) {
                                        ReplayExportUiState.Error("Replay sharing could not be started.")
                                    }
                                }
                            },
                        )
                    }

                    item {
                        RunDetailsSummaryCard(
                            run = run,
                            analyticsSnapshot = analyticsSnapshot,
                            surface = palette.surface,
                        )
                    }

                    item {
                        RunSpeedChartCard(
                            analyticsSnapshot = analyticsSnapshot,
                            surface = palette.surfaceAlt,
                            accent = palette.accent,
                        )
                    }

                    item {
                        RunSplitListCard(
                            analyticsSnapshot = analyticsSnapshot,
                            surface = palette.surface,
                            accent = palette.accent,
                        )
                    }

                    item {
                        RunInsightsAnalyticsCard(
                            analyticsSnapshot = analyticsSnapshot,
                            surface = palette.surfaceAlt,
                            accent = palette.accent,
                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun RunDetailsHeaderCard(
    run: RunEntity,
    surface: Color,
    tone: Color,
    chipColor: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${run.type.label} • ${formatRunDate(run.startTime)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${formatRunTime(run.startTime)} ${if (run.endTime != null) "• completed" else "• active or paused"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RunChip(
                    text = formatDistance(run.distanceMeters),
                    background = chipColor,
                    color = tone,
                )
                RunChip(
                    text = formatDuration(run.durationSec),
                    background = chipColor,
                    color = tone,
                )
            }
        }
    }
}

@Composable
private fun RunDetailsRouteCard(
    run: RunEntity,
    replayFrames: List<RunReplayFrame>,
    replayFrameIndex: Int,
    currentReplayFrame: RunReplayFrame?,
    replayPlaying: Boolean,
    surface: Color,
    accent: Color,
    onReplayFrameIndexChange: (Int) -> Unit,
    onReplayReset: () -> Unit,
    onReplayToggle: () -> Unit,
    exportUiState: ReplayExportUiState,
    onExportReplay: () -> Unit,
) {
    val replayUiModel = buildRunDetailsReplayUiModel(
        run = run,
        replayFrames = replayFrames,
        currentReplayFrame = currentReplayFrame,
        exportUiState = exportUiState,
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Route",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (run.polyline.isBlank()) {
                Text(
                    "This ${run.type.label.lowercase()} session does not have enough route data yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                ) {
                    RunMapView(
                        polylineEncoded = run.polyline,
                        replayPointIndex = if (replayUiModel.hasReplay) replayUiModel.replayPointIndex else null,
                        replayViewport = if (replayUiModel.hasReplay) currentReplayFrame?.cameraViewport else null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    "The details screen now owns replay and route review so the dashboard can stay focused.",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )

                if (replayUiModel.hasReplay) {
                    Text("Replay", style = MaterialTheme.typography.titleSmall, color = accent)
                    Slider(
                        value = replayFrameIndex.toFloat(),
                        onValueChange = { onReplayFrameIndexChange(it.toInt()) },
                        valueRange = 0f..replayFrames.lastIndex.toFloat(),
                        modifier = Modifier.semantics { contentDescription = "Run details replay position slider" },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Frame ${replayFrameIndex + 1} / ${replayFrames.size}",
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (replayUiModel.replaySummary != null) {
                                Text(
                                    replayUiModel.replaySummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onReplayReset,
                                modifier = Modifier.semantics { contentDescription = "Reset run details replay" },
                            ) {
                                Text("Reset")
                            }
                            Button(
                                onClick = onReplayToggle,
                                modifier = Modifier.semantics {
                                    contentDescription = if (replayPlaying) "Pause run details replay" else "Play run details replay"
                                },
                            ) {
                                Text(if (replayPlaying) "Pause Replay" else "Play Replay")
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Share replay", style = MaterialTheme.typography.titleSmall, color = accent)
                            when (exportUiState) {
                                ReplayExportUiState.Idle -> Text(
                                    replayUiModel.exportMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                ReplayExportUiState.Exporting -> Text(
                                    replayUiModel.exportMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                is ReplayExportUiState.Error -> Text(
                                    replayUiModel.exportMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )

                                is ReplayExportUiState.Ready -> Text(
                                    replayUiModel.exportMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (replayUiModel.showExportProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Button(
                                onClick = onExportReplay,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Export run replay video" },
                            ) {
                                Text(replayUiModel.exportButtonLabel)
                            }
                        }
                    }
                } else {
                    Text(
                        "Replay becomes available after a completed session with enough route points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal data class RunDetailsReplayUiModel(
    val hasReplay: Boolean,
    val replayPointIndex: Int?,
    val replaySummary: String?,
    val exportMessage: String,
    val exportButtonLabel: String,
    val showExportProgress: Boolean,
)

internal fun buildRunDetailsReplayUiModel(
    run: RunEntity,
    replayFrames: List<RunReplayFrame>,
    currentReplayFrame: RunReplayFrame?,
    exportUiState: ReplayExportUiState,
): RunDetailsReplayUiModel {
    val hasReplay = run.endTime != null && replayFrames.size > 1
    val replayPointIndex = currentReplayFrame?.visiblePointCount?.minus(1)?.coerceAtLeast(0)
    val replaySummary = currentReplayFrame?.let {
        "${formatDistance(it.distanceMeters)} • ${formatDuration((it.displayElapsedMs / 1000L).coerceAtLeast(0L))}"
    }
    val exportMessage = when (exportUiState) {
        ReplayExportUiState.Idle -> "Create a deterministic 15-second MP4 of this session."
        ReplayExportUiState.Exporting -> "Rendering map frames and encoding video..."
        is ReplayExportUiState.Error -> exportUiState.message
        is ReplayExportUiState.Ready -> "Replay video is ready and shareable."
    }
    return RunDetailsReplayUiModel(
        hasReplay = hasReplay,
        replayPointIndex = if (hasReplay) replayPointIndex else null,
        replaySummary = replaySummary,
        exportMessage = exportMessage,
        exportButtonLabel = if (exportUiState is ReplayExportUiState.Ready) "Share Again" else "Export MP4",
        showExportProgress = exportUiState is ReplayExportUiState.Exporting,
    )
}

internal sealed interface ReplayExportUiState {
    data object Idle : ReplayExportUiState
    data object Exporting : ReplayExportUiState
    data class Ready(val result: RunReplayExportResult) : ReplayExportUiState
    data class Error(val message: String) : ReplayExportUiState
}

private fun launchShareIntent(
    context: Context,
    result: RunReplayExportResult,
) {
    val shareIntent = result.shareIntent.apply {
        if (context.findActivity() == null) {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(shareIntent)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun RunDetailsSummaryCard(
    run: RunEntity,
    analyticsSnapshot: RunAnalysisSnapshot?,
    surface: Color,
) {
    val summaryItems = listOf(
        "Distance" to formatDistance(run.distanceMeters),
        "Moving time" to formatDuration(analyticsSnapshot?.movingDurationSec ?: run.durationSec),
        "Elapsed time" to formatDuration(analyticsSnapshot?.elapsedDurationSec ?: run.durationSec),
        "Average speed" to String.format("%.2f km/h", run.avgSpeedMps * 3.6),
        "Average pace" to (analyticsSnapshot?.avgPaceSecPerKm?.let(::formatPace) ?: runPaceLabel(run) ?: "--"),
        "Max speed" to String.format("%.2f km/h", (analyticsSnapshot?.maxSpeedMps ?: 0.0) * 3.6),
        "Calories" to String.format("%.0f kcal", analyticsSnapshot?.calories ?: run.calories),
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            summaryItems.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    row.forEach { (label, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RunSpeedChartCard(
    analyticsSnapshot: RunAnalysisSnapshot?,
    surface: Color,
    accent: Color,
) {
    val samples = analyticsSnapshot?.speedSamples.orEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Speed profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (samples.isEmpty()) {
                Text(
                    "Record more timed route points to build a speed profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Average speed by segment window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RunSpeedChart(
                    samples = samples,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                )
            }
        }
    }
}

@Composable
private fun RunSplitListCard(
    analyticsSnapshot: RunAnalysisSnapshot?,
    surface: Color,
    accent: Color,
) {
    val splits = analyticsSnapshot?.splits.orEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Splits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (splits.isEmpty()) {
                Text(
                    "Splits appear once the run has enough distance and timed route points.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                splits.forEach { split ->
                    RunSplitRow(split = split, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun RunSplitRow(
    split: RunSplitSummary,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                if (split.distanceMeters >= 950.0) "Km ${split.index}" else "Final split",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                formatDistance(split.distanceMeters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatDuration(split.durationSec),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                formatPace(split.paceSecPerKm),
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun RunInsightsAnalyticsCard(
    analyticsSnapshot: RunAnalysisSnapshot?,
    surface: Color,
    accent: Color,
) {
    val insights = analyticsSnapshot?.insights.orEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Run insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            insights.forEach { insight ->
                Text(
                    "• $insight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (insights.isEmpty()) {
                Text(
                    "More route history will unlock stronger pacing and split insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "These analytics now come from run-point timestamps rather than only the summary row.",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun RunSpeedChart(
    samples: List<RunSpeedSample>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val maxSpeed = max(samples.maxOfOrNull { it.speedMps } ?: 1.0, 1.0)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val chartHeight = size.height
                    val chartWidth = size.width
                    val spacing = chartWidth / max(samples.size, 1)
                    val path = Path()

                    samples.forEachIndexed { index, sample ->
                        val x = spacing * index + spacing / 2f
                        val y = chartHeight - ((sample.speedMps / maxSpeed).toFloat() * chartHeight)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        drawCircle(
                            color = accent,
                            radius = 8f,
                            center = Offset(x, y),
                        )
                    }

                    drawPath(
                        path = path,
                        color = accent,
                        style = Stroke(width = 6f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                samples.forEach { sample ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${"%.1f".format(sample.speedMps * 3.6)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(sample.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunUpcomingFeatureCard(
    title: String,
    surface: Color,
    accent: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent),
            )
        }
    }
}
