package com.example.productivityapp.run

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class RunReplayFrame(
    val visiblePointCount: Int,
    val normalizedProgress: Float,
    val displayElapsedMs: Long,
    val distanceMeters: Double,
    val cameraViewport: RunMapViewport?,
    val frameDurationMs: Long,
)

class RunReplayTimelineBuilder(
    private val viewportCalculator: RunMapViewportCalculator = RunMapViewportCalculator(),
    private val defaultPointIntervalMs: Long = 1_000L,
) {
    fun buildInteractiveTimeline(
        points: List<RunReplayHelper.Point>,
        durationMs: Long = 12_000L,
        maxFrames: Int = 90,
    ): List<RunReplayFrame> {
        if (points.isEmpty()) return emptyList()
        val frameCount = min(max(points.size, 1), maxFrames.coerceAtLeast(1))
        return buildTimeline(points, frameCount = frameCount, durationMs = durationMs)
    }

    fun buildFixedDurationTimeline(
        points: List<RunReplayHelper.Point>,
        durationMs: Long = 15_000L,
        fps: Int = 30,
    ): List<RunReplayFrame> {
        if (points.isEmpty()) return emptyList()
        val frameCount = max(1, ((durationMs / 1_000.0) * fps).roundToInt())
        return buildTimeline(points, frameCount = frameCount, durationMs = durationMs)
    }

    private fun buildTimeline(
        points: List<RunReplayHelper.Point>,
        frameCount: Int,
        durationMs: Long,
    ): List<RunReplayFrame> {
        if (points.isEmpty()) return emptyList()

        val enrichedPoints = enrichPoints(points)
        val totalElapsedMs = enrichedPoints.last().elapsedMs
        val perFrameDurationMs = if (frameCount <= 1) 0L else max(60L, durationMs / (frameCount - 1))
        var sourceIndex = 0

        return List(frameCount) { frameIndex ->
            val normalizedTime = if (frameCount <= 1) 1.0 else frameIndex.toDouble() / (frameCount - 1).toDouble()
            val targetElapsedMs = if (frameIndex == frameCount - 1) {
                totalElapsedMs
            } else {
                (totalElapsedMs * normalizedTime).roundToLong()
            }
            while (
                sourceIndex < enrichedPoints.lastIndex &&
                enrichedPoints[sourceIndex + 1].elapsedMs <= targetElapsedMs
            ) {
                sourceIndex += 1
            }
            val sourcePoint = enrichedPoints[sourceIndex]
            val visibleCount = sourceIndex + 1
            val viewport = viewportCalculator.buildViewport(
                south = sourcePoint.south,
                west = sourcePoint.west,
                north = sourcePoint.north,
                east = sourcePoint.east,
            )

            RunReplayFrame(
                visiblePointCount = visibleCount,
                normalizedProgress = if (enrichedPoints.size <= 1) 1f else sourceIndex.toFloat() / enrichedPoints.lastIndex.toFloat(),
                displayElapsedMs = sourcePoint.elapsedMs,
                distanceMeters = sourcePoint.distanceMeters,
                cameraViewport = viewport,
                frameDurationMs = if (frameIndex < frameCount - 1) perFrameDurationMs else 0L,
            )
        }
    }

    private fun enrichPoints(points: List<RunReplayHelper.Point>): List<EnrichedReplayPoint> {
        if (points.isEmpty()) return emptyList()

        val enriched = ArrayList<EnrichedReplayPoint>(points.size)
        var elapsedMs = 0L
        var distanceMeters = 0.0
        var south = points.first().lat
        var north = points.first().lat
        var west = points.first().lon
        var east = points.first().lon

        points.forEachIndexed { index, point ->
            if (index > 0) {
                val previous = points[index - 1]
                val hasSequentialTimestamps = point.tsMs > 0L &&
                    previous.tsMs > 0L &&
                    point.tsMs > previous.tsMs
                elapsedMs += if (hasSequentialTimestamps) {
                    point.tsMs - previous.tsMs
                } else {
                    defaultPointIntervalMs
                }
                distanceMeters += viewportCalculator.distanceMeters(
                    previous.lat,
                    previous.lon,
                    point.lat,
                    point.lon,
                )
            }

            south = min(south, point.lat)
            north = max(north, point.lat)
            west = min(west, point.lon)
            east = max(east, point.lon)

            enriched += EnrichedReplayPoint(
                point = point,
                elapsedMs = elapsedMs,
                distanceMeters = distanceMeters,
                south = south,
                west = west,
                north = north,
                east = east,
            )
        }

        return enriched
    }

    private data class EnrichedReplayPoint(
        val point: RunReplayHelper.Point,
        val elapsedMs: Long,
        val distanceMeters: Double,
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
    )
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
