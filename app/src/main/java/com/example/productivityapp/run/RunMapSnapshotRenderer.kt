package com.example.productivityapp.run

import android.content.Context
import android.graphics.Bitmap
import com.example.productivityapp.ui.run.RunMapStyleProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.Style
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.snapshotter.MapSnapshotter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.graphics.drawable.toBitmap

interface RunMapSnapshotRenderer {
    suspend fun renderFrame(
        frame: RunReplayRenderedFrame,
        allPoints: List<RunReplayHelper.Point>,
        isDarkTheme: Boolean,
        config: RunReplayExportConfig,
    ): Bitmap
}

class MapLibreRunMapSnapshotRenderer(
    context: Context,
) : RunMapSnapshotRenderer {
    private val appContext = context.applicationContext
    private val overlayRenderer: RunReplayOverlayRenderer

    init {
        // Load start/end drawable resources as bitmaps for crisp overlay icons
        val startDrawable = androidx.core.content.ContextCompat.getDrawable(appContext, com.example.productivityapp.R.drawable.ic_replay_start)
        val endDrawable = androidx.core.content.ContextCompat.getDrawable(appContext, com.example.productivityapp.R.drawable.ic_replay_end)

        fun drawableToBitmap(drawable: android.graphics.drawable.Drawable?): android.graphics.Bitmap? {
            drawable ?: return null
            if (drawable is android.graphics.drawable.BitmapDrawable) return drawable.bitmap
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 24
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 24
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, c.width, c.height)
            drawable.draw(c)
            return bmp
        }

        val startBmp = drawableToBitmap(startDrawable)
        val endBmp = drawableToBitmap(endDrawable)
        overlayRenderer = RunReplayOverlayRenderer(startIcon = startBmp, endIcon = endBmp)
    }

    override suspend fun renderFrame(
        frame: RunReplayRenderedFrame,
        allPoints: List<RunReplayHelper.Point>,
        isDarkTheme: Boolean,
        config: RunReplayExportConfig,
    ): Bitmap = withContext(Dispatchers.Main.immediate) {
        val viewport = frame.frame.cameraViewport
            ?: throw RunReplayExportException.SnapshotRenderingFailed("Replay frame is missing a camera viewport.")

        try {
            MapLibre.getInstance(appContext)
        } catch (error: IllegalStateException) {
            throw RunReplayExportException.SnapshotRenderingFailed("Map engine could not be initialized for replay export.", error)
        }

        val options = MapSnapshotter.Options(config.widthPx, config.heightPx)
            .withStyleBuilder(Style.Builder().fromUri(RunMapStyleProvider.styleUrl(isDarkTheme)))
            .withRegion(LatLngBounds.from(viewport.north, viewport.east, viewport.south, viewport.west))
            .withLogo(false)

        suspendCancellableCoroutine { continuation ->
            val snapshotter = try {
                MapSnapshotter(appContext, options)
            } catch (error: IllegalStateException) {
                continuation.resumeWithException(
                    RunReplayExportException.SnapshotRenderingFailed(
                        "Replay snapshotter could not be created.",
                        error,
                    )
                )
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                snapshotter.cancel()
            }

            snapshotter.start(
                { snapshot ->
                    try {
                        val rendered = overlayRenderer.render(
                            snapshot = snapshot,
                            allPoints = allPoints,
                            visiblePointCount = frame.frame.visiblePointCount,
                            overlayStats = frame.overlayStats,
                        )
                        continuation.resume(rendered)
                    } catch (error: IllegalArgumentException) {
                        continuation.resumeWithException(
                            RunReplayExportException.SnapshotRenderingFailed(
                                "Replay snapshot overlay rendering failed.",
                                error,
                            )
                        )
                    }
                },
                { errorMessage ->
                    continuation.resumeWithException(
                        RunReplayExportException.SnapshotRenderingFailed(
                            errorMessage.ifBlank { "Replay snapshot rendering failed." }
                        )
                    )
                }
            )
        }
    }
}
