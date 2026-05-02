package com.example.productivityapp.run

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.snapshotter.MapSnapshot
import kotlin.math.min

class RunReplayOverlayRenderer(
    private val startIcon: Bitmap? = null,
    private val endIcon: Bitmap? = null,
) {

    fun render(
        snapshot: MapSnapshot,
        allPoints: List<RunReplayHelper.Point>,
        visiblePointCount: Int,
        overlayStats: RunReplayOverlayStats,
    ): Bitmap {
        val base = snapshot.bitmap
        val output = base.copy(Bitmap.Config.ARGB_8888, true) ?: createBitmap(base.width, base.height)
        val canvas = Canvas(output)

        if (output.width != base.width || output.height != base.height) {
            canvas.drawBitmap(base, 0f, 0f, null)
        }

        drawAllRoute(canvas, snapshot, allPoints)
        drawVisibleRoute(
            canvas = canvas,
            snapshot = snapshot,
            points = allPoints,
            visiblePointCount = visiblePointCount.coerceAtLeast(1),
        )
        drawStartEndMarkers(canvas, snapshot, allPoints)
        drawProgressMarker(canvas, snapshot, allPoints.getOrNull((visiblePointCount - 1).coerceAtLeast(0)))
        drawTopOverlay(canvas, overlayStats)
        drawAttribution(canvas, snapshot.attributions.joinToString(" • "))
        return output
    }

    private fun drawAllRoute(
        canvas: Canvas,
        snapshot: MapSnapshot,
        points: List<RunReplayHelper.Point>,
    ) {
        if (points.size < 2) return
        val path = buildPath(snapshot, points) ?: return
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            // soft light-green glow
            color = Color.argb(80, 123, 227, 138)
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            // thinner solid light-green route
            color = Color.argb(200, 123, 227, 138)
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, basePaint)
    }

    private fun drawVisibleRoute(
        canvas: Canvas,
        snapshot: MapSnapshot,
        points: List<RunReplayHelper.Point>,
        visiblePointCount: Int,
    ) {
        if (visiblePointCount < 2 || points.size < 2) return
        val path = buildPath(snapshot, points, visiblePointCount) ?: return
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(110, 123, 227, 138)
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#7BE38A")
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, routePaint)
    }

    private fun drawProgressMarker(
        canvas: Canvas,
        snapshot: MapSnapshot,
        point: RunReplayHelper.Point?,
    ) {
        point ?: return
        val pixel = snapshot.pixelForLatLng(LatLng(point.lat, point.lon))
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(110, 255, 255, 255)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#7BE38A")
        }
        canvas.drawCircle(pixel.x, pixel.y, 16f, haloPaint)
        canvas.drawCircle(pixel.x, pixel.y, 9f, fillPaint)
    }

    private fun drawStartEndMarkers(canvas: Canvas, snapshot: MapSnapshot, points: List<RunReplayHelper.Point>) {
        if (points.isEmpty()) return
        val startPixel = snapshot.pixelForLatLng(LatLng(points.first().lat, points.first().lon))
        val endPixel = snapshot.pixelForLatLng(LatLng(points.last().lat, points.last().lon))

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(110, 255, 255, 255)
        }

        // draw halos
        canvas.drawCircle(startPixel.x, startPixel.y, 14f, haloPaint)
        canvas.drawCircle(endPixel.x, endPixel.y, 14f, haloPaint)

        // draw provided icons if available, otherwise fallback to emoji text
        val iconSizePx = 28
        startIcon?.let { bmp ->
            val scaled = Bitmap.createScaledBitmap(bmp, iconSizePx, iconSizePx, true)
            canvas.drawBitmap(scaled, startPixel.x - scaled.width / 2f, startPixel.y - scaled.height / 2f, null)
        } ?: run {
            val startPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#7BE38A")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val startText = "\u25B6"
            val startTextWidth = startPaint.measureText(startText)
            canvas.drawText(startText, startPixel.x - startTextWidth / 2f, startPixel.y + 10f, startPaint)
        }

        endIcon?.let { bmp ->
            val scaled = Bitmap.createScaledBitmap(bmp, iconSizePx, iconSizePx, true)
            canvas.drawBitmap(scaled, endPixel.x - scaled.width / 2f, endPixel.y - scaled.height / 2f, null)
        } ?: run {
            val endPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#FF6B6B")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val endText = "\uD83C\uDFC1"
            val endTextWidth = endPaint.measureText(endText)
            canvas.drawText(endText, endPixel.x - endTextWidth / 2f, endPixel.y + 10f, endPaint)
        }
    }

    private fun drawTopOverlay(
        canvas: Canvas,
        overlayStats: RunReplayOverlayStats,
    ) {
        val width = canvas.width.toFloat()
        val cardWidth = width - 64f
        val cardRect = RectF(32f, 28f, 32f + cardWidth, 128f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 14, 18, 36)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 245, 248, 255)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 212, 219, 255)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
        canvas.drawText("Run Replay", 58f, 62f, labelPaint)
        canvas.drawText(overlayStats.distanceLabel, 58f, 104f, valuePaint)

        val rightBlockX = cardRect.right - 210f
        canvas.drawText("Elapsed", rightBlockX, 62f, smallPaint)
        canvas.drawText(overlayStats.elapsedLabel, rightBlockX, 96f, labelPaint)
        canvas.drawText(overlayStats.paceLabel, rightBlockX, 123f, smallPaint)
    }

    private fun drawAttribution(canvas: Canvas, attribution: String) {
        if (attribution.isBlank()) return
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = 18f
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 10, 12, 24)
        }
        val paddingHorizontal = 18f
        val paddingVertical = 10f
        val maxWidth = canvas.width - 40f
        val displayText = attribution.take(120)
        val textWidth = min(textPaint.measureText(displayText), maxWidth.toFloat())
        val rect = RectF(
            20f,
            canvas.height - 46f,
            20f + textWidth + paddingHorizontal * 2,
            canvas.height - 12f,
        )
        canvas.drawRoundRect(rect, 18f, 18f, bgPaint)
        canvas.drawText(displayText, rect.left + paddingHorizontal, rect.bottom - paddingVertical, textPaint)
    }

    private fun buildPath(
        snapshot: MapSnapshot,
        points: List<RunReplayHelper.Point>,
        pointCount: Int = points.size,
    ): Path? {
        if (points.size < 2 || pointCount < 2) return null
        val safePointCount = pointCount.coerceAtMost(points.size)
        return Path().apply {
            for (index in 0 until safePointCount) {
                val point = points[index]
                val pixel = snapshot.pixelForLatLng(LatLng(point.lat, point.lon))
                if (index == 0) moveTo(pixel.x, pixel.y) else lineTo(pixel.x, pixel.y)
            }
        }
    }
}
