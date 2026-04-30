package com.example.productivityapp.run

import android.content.Intent
import android.net.Uri
import com.example.productivityapp.data.entities.RunEntity

data class RunReplayExportConfig(
    val widthPx: Int = 720,
    val heightPx: Int = 720,
    val durationMs: Long = 15_000L,
    val fps: Int = 30,
    val bitrateMbps: Int = 6,
)

data class RunReplayOverlayStats(
    val distanceLabel: String,
    val elapsedLabel: String,
    val paceLabel: String,
)

data class RunReplayRenderedFrame(
    val frame: RunReplayFrame,
    val overlayStats: RunReplayOverlayStats,
)

data class RunReplayShareAsset(
    val uri: Uri,
    val fileName: String,
    val shareIntent: Intent,
)

data class RunReplayExportResult(
    val fileUri: Uri,
    val shareIntent: Intent,
    val frameCount: Int,
)

internal fun buildReplayShareFileName(run: RunEntity): String {
    return "run-replay-${run.id}-${run.startTime}.mp4"
}
