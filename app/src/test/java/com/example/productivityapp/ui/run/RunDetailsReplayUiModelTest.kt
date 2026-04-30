package com.example.productivityapp.ui.run

import android.content.Intent
import android.net.Uri
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.run.RunMapViewport
import com.example.productivityapp.run.RunReplayExportResult
import com.example.productivityapp.run.RunReplayFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunDetailsReplayUiModelTest {

    @Test
    fun incompleteRun_withoutEnoughFrames_disablesReplay() {
        val model = buildRunDetailsReplayUiModel(
            run = runEntity(endTime = null),
            replayFrames = listOf(replayFrame(visiblePointCount = 1)),
            currentReplayFrame = replayFrame(visiblePointCount = 1),
            exportUiState = ReplayExportUiState.Idle,
        )

        assertFalse(model.hasReplay)
        assertNull(model.replayPointIndex)
        assertEquals("Create a deterministic 15-second MP4 of this run.", model.exportMessage)
        assertEquals("Export MP4", model.exportButtonLabel)
    }

    @Test
    fun completedRun_withReplayFrame_buildsSummaryAndReplayIndex() {
        val currentFrame = replayFrame(
            visiblePointCount = 4,
            distanceMeters = 2450.0,
            displayElapsedMs = 605_000L,
        )
        val model = buildRunDetailsReplayUiModel(
            run = runEntity(endTime = 2_000L),
            replayFrames = listOf(replayFrame(1), replayFrame(2), currentFrame),
            currentReplayFrame = currentFrame,
            exportUiState = ReplayExportUiState.Idle,
        )

        assertTrue(model.hasReplay)
        assertEquals(3, model.replayPointIndex)
        assertEquals("2.45 km • 10:05", model.replaySummary)
    }

    @Test
    fun exportStates_surfaceExpectedMessagesAndActions() {
        val readyState = ReplayExportUiState.Ready(
            RunReplayExportResult(
                fileUri = Uri.parse("content://run/replay.mp4"),
                shareIntent = Intent(Intent.ACTION_SEND),
                frameCount = 10,
            )
        )
        val readyModel = buildRunDetailsReplayUiModel(
            run = runEntity(endTime = 2_000L),
            replayFrames = listOf(replayFrame(1), replayFrame(2)),
            currentReplayFrame = replayFrame(2),
            exportUiState = readyState,
        )
        val exportingModel = buildRunDetailsReplayUiModel(
            run = runEntity(endTime = 2_000L),
            replayFrames = listOf(replayFrame(1), replayFrame(2)),
            currentReplayFrame = replayFrame(2),
            exportUiState = ReplayExportUiState.Exporting,
        )
        val errorModel = buildRunDetailsReplayUiModel(
            run = runEntity(endTime = 2_000L),
            replayFrames = listOf(replayFrame(1), replayFrame(2)),
            currentReplayFrame = replayFrame(2),
            exportUiState = ReplayExportUiState.Error("Replay export failed."),
        )

        assertEquals("Replay video is ready and shareable.", readyModel.exportMessage)
        assertEquals("Share Again", readyModel.exportButtonLabel)
        assertEquals("Rendering map frames and encoding video...", exportingModel.exportMessage)
        assertTrue(exportingModel.showExportProgress)
        assertEquals("Replay export failed.", errorModel.exportMessage)
    }

    private fun runEntity(endTime: Long?): RunEntity = RunEntity(
        id = 7L,
        startTime = 1_000L,
        endTime = endTime,
        distanceMeters = 2_500.0,
        durationSec = 650L,
        avgSpeedMps = 3.84,
        calories = 120.0,
        polyline = "encoded",
    )

    private fun replayFrame(
        visiblePointCount: Int,
        distanceMeters: Double = 0.0,
        displayElapsedMs: Long = 0L,
    ): RunReplayFrame = RunReplayFrame(
        visiblePointCount = visiblePointCount,
        normalizedProgress = 1f,
        displayElapsedMs = displayElapsedMs,
        distanceMeters = distanceMeters,
        cameraViewport = RunMapViewport(0.0, 0.0, 1.0, 1.0, 72),
        frameDurationMs = 120L,
    )
}
