package com.example.productivityapp.run

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import com.example.productivityapp.data.repository.RunRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RunReplayExporterTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun exportRunReplay_rendersFramesEncodesVideoAndBuildsShareIntent() = runTest {
        val repository = FakeRunRepository()
        val renderer = FakeSnapshotRenderer()
        val encoder = FakeVideoEncoder()
        val shareWriter = FakeShareFileWriter(appContext.cacheDir)
        val exporter = RunReplayExporter(
            context = appContext,
            runRepository = repository,
            snapshotRenderer = renderer,
            videoEncoder = encoder,
            shareFileWriter = shareWriter,
        )

        val result = exporter.exportRunReplay(
            runId = 7L,
            isDarkTheme = false,
            analyticsSnapshot = null,
            config = RunReplayExportConfig(durationMs = 2_000L, fps = 5, widthPx = 200, heightPx = 200),
        )

        assertEquals(10, result.frameCount)
        assertEquals(10, renderer.renderedFrames)
        assertEquals(10, encoder.encodedFrames)
        assertEquals("video/mp4", result.shareIntent.type)
    }

    @Test(expected = RunReplayExportException.InsufficientRouteData::class)
    fun exportRunReplay_rejectsRunsWithoutEnoughRoutePoints() = runTest {
        val repository = FakeRunRepository(points = listOf(RunPointEntity(runId = 7L, lat = 10.0, lon = 10.0, tsMs = 1_000L)))
        val exporter = RunReplayExporter(
            context = appContext,
            runRepository = repository,
            snapshotRenderer = FakeSnapshotRenderer(),
            videoEncoder = FakeVideoEncoder(),
            shareFileWriter = FakeShareFileWriter(appContext.cacheDir),
        )

        exporter.exportRunReplay(runId = 7L, isDarkTheme = false, analyticsSnapshot = null)
    }

    private class FakeRunRepository(
        private val run: RunEntity = RunEntity(
            id = 7L,
            startTime = 1_000L,
            endTime = 11_000L,
            distanceMeters = 1_500.0,
            durationSec = 600L,
            avgSpeedMps = 2.5,
            calories = 100.0,
            polyline = "",
        ),
        private val points: List<RunPointEntity> = listOf(
            RunPointEntity(runId = 7L, lat = 12.9716, lon = 77.5946, tsMs = 1_000L),
            RunPointEntity(runId = 7L, lat = 12.9720, lon = 77.5951, tsMs = 3_000L),
            RunPointEntity(runId = 7L, lat = 12.9790, lon = 77.6060, tsMs = 7_000L),
        ),
    ) : RunRepository {
        override fun observeRuns(): Flow<List<RunEntity>> = emptyFlow()
        override fun observeLatestRun(): Flow<RunEntity?> = emptyFlow()
        override fun observeRun(id: Long): Flow<RunEntity?> = emptyFlow()
        override fun observeRunPoints(runId: Long): Flow<List<RunPointEntity>> = emptyFlow()
        override suspend fun getRunById(id: Long): RunEntity? = run
        override suspend fun getRunPoints(runId: Long): List<RunPointEntity> = points
        override suspend fun startRun(run: RunEntity): Long = run.id
        override suspend fun updateRun(run: RunEntity) = Unit
        override suspend fun finishRun(runId: Long) = Unit
        override suspend fun addLocationPoint(point: RunPointEntity) = Unit
    }

    private class FakeSnapshotRenderer : RunMapSnapshotRenderer {
        var renderedFrames: Int = 0

        override suspend fun renderFrame(
            frame: RunReplayRenderedFrame,
            allPoints: List<RunReplayHelper.Point>,
            isDarkTheme: Boolean,
            config: RunReplayExportConfig,
        ): android.graphics.Bitmap {
            renderedFrames += 1
            return android.graphics.Bitmap.createBitmap(config.widthPx, config.heightPx, android.graphics.Bitmap.Config.ARGB_8888)
        }
    }

    private class FakeVideoEncoder : RunReplayVideoEncoder {
        var encodedFrames: Int = 0

        override suspend fun encodeVideo(
            outputFile: File,
            config: RunReplayExportConfig,
            frameCount: Int,
            frameProvider: suspend (Int) -> android.graphics.Bitmap,
        ): File {
            repeat(frameCount) {
                frameProvider(it)
                encodedFrames += 1
            }
            outputFile.parentFile?.mkdirs()
            outputFile.writeBytes(byteArrayOf(1, 2, 3))
            return outputFile
        }
    }

    private class FakeShareFileWriter(cacheDir: File) : RunShareFileWriter {
        private val root = File(cacheDir, "run-replays-test").apply { mkdirs() }

        override fun prepareShareAsset(fileName: String): File = File(root, fileName)

        override fun buildShareAsset(outputFile: File): RunReplayShareAsset {
            assertTrue(outputFile.exists())
            return RunReplayShareAsset(
                uri = Uri.fromFile(outputFile),
                fileName = outputFile.name,
                shareIntent = Intent(Intent.ACTION_SEND).apply { type = "video/mp4" },
            )
        }
    }
}
