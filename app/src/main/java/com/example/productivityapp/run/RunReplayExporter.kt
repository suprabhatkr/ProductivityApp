package com.example.productivityapp.run

import android.content.Context
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.repository.RunRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class RunReplayExporter(
    private val context: Context,
    private val runRepository: RunRepository,
    private val snapshotRenderer: RunMapSnapshotRenderer,
    private val videoEncoder: RunReplayVideoEncoder,
    private val shareFileWriter: RunShareFileWriter,
    private val timelineBuilder: RunReplayTimelineBuilder = RunReplayTimelineBuilder(),
) {
    suspend fun exportRunReplay(
        runId: Long,
        isDarkTheme: Boolean,
        analyticsSnapshot: RunAnalysisSnapshot?,
        config: RunReplayExportConfig = RunReplayExportConfig(),
    ): RunReplayExportResult = withContext(Dispatchers.Default) {
        val run = runRepository.getRunById(runId) ?: throw RunReplayExportException.RunNotFound(runId)
        val storedPoints = runRepository.getRunPoints(runId)
        val replayPoints = if (storedPoints.isNotEmpty()) {
            RunReplayHelper.fromPointEntities(storedPoints)
        } else {
            RunReplayHelper.decodeEncodedPolyline(run.polyline)
        }
        if (replayPoints.size < 2) {
            throw RunReplayExportException.InsufficientRouteData()
        }

        val exportFrames = timelineBuilder.buildFixedDurationTimeline(
            points = replayPoints,
            durationMs = config.durationMs,
            fps = config.fps,
        )
        if (exportFrames.isEmpty()) {
            throw RunReplayExportException.InsufficientRouteData()
        }

        val renderedFrames = exportFrames.map { frame ->
            RunReplayRenderedFrame(
                frame = frame,
                overlayStats = frame.buildOverlayStats(run, analyticsSnapshot),
            )
        }

        val outputFile = shareFileWriter.prepareShareAsset(buildReplayShareFileName(run))
        videoEncoder.encodeVideo(
            outputFile = outputFile,
            config = config,
            frameCount = renderedFrames.size,
        ) { index ->
            snapshotRenderer.renderFrame(
                frame = renderedFrames[index],
                allPoints = replayPoints,
                isDarkTheme = isDarkTheme,
                config = config,
            )
        }
        val shareAsset = shareFileWriter.buildShareAsset(outputFile)
        RunReplayExportResult(
            fileUri = shareAsset.uri,
            shareIntent = shareAsset.shareIntent,
            frameCount = renderedFrames.size,
        )
    }

    private fun RunReplayFrame.buildOverlayStats(
        run: RunEntity,
        analyticsSnapshot: RunAnalysisSnapshot?,
    ): RunReplayOverlayStats {
        return RunReplayOverlayStats(
            distanceLabel = formatDistance(distanceMeters),
            elapsedLabel = formatDuration((displayElapsedMs / 1_000L).coerceAtLeast(0L)),
            paceLabel = analyticsSnapshot?.avgPaceSecPerKm?.let(::formatPace)
                ?: runPaceLabel(run)
                ?: "-- pace",
        )
    }

    private fun formatDistance(distanceMeters: Double): String {
        return String.format(Locale.US, "%.2f km", distanceMeters / 1_000.0)
    }

    private fun formatDuration(durationSec: Long): String {
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun formatPace(secondsPerKm: Double): String {
        if (!secondsPerKm.isFinite() || secondsPerKm <= 0.0) return "-- pace"
        val totalSeconds = secondsPerKm.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
    }

    private fun runPaceLabel(run: RunEntity): String? {
        if (run.distanceMeters <= 0.0 || run.durationSec <= 0L) return null
        val secondsPerKm = run.durationSec / (run.distanceMeters / 1_000.0)
        return formatPace(secondsPerKm)
    }
}
