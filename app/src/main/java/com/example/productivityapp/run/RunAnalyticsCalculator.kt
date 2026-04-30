package com.example.productivityapp.run

import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RunSpeedSample(
    val label: String,
    val speedMps: Double,
)

data class RunSplitSummary(
    val index: Int,
    val distanceMeters: Double,
    val durationSec: Long,
    val paceSecPerKm: Double,
)

data class RunAnalysisSnapshot(
    val movingDurationSec: Long,
    val elapsedDurationSec: Long,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val avgPaceSecPerKm: Double?,
    val calories: Double,
    val totalDistanceMeters: Double,
    val speedSamples: List<RunSpeedSample>,
    val splits: List<RunSplitSummary>,
    val insights: List<String>,
)

class RunAnalyticsCalculator {

    fun buildSnapshot(
        run: RunEntity,
        points: List<RunPointEntity>,
    ): RunAnalysisSnapshot {
        val sortedPoints = points.sortedBy { it.tsMs }
        val segments = buildSegments(sortedPoints)
        val elapsedDurationSec = when {
            run.endTime != null -> ((run.endTime - run.startTime) / 1000L).coerceAtLeast(run.durationSec)
            else -> run.durationSec
        }
        val avgPaceSecPerKm = if (run.distanceMeters > 0.0 && run.durationSec > 0L) {
            run.durationSec / (run.distanceMeters / 1000.0)
        } else {
            null
        }
        val maxSpeedMps = max(run.avgSpeedMps, segments.maxOfOrNull { it.speedMps } ?: 0.0)
        val speedSamples = buildSpeedSamples(segments)
        val splits = buildSplits(run, segments)
        val insights = buildInsights(run, elapsedDurationSec, maxSpeedMps, splits)

        return RunAnalysisSnapshot(
            movingDurationSec = run.durationSec,
            elapsedDurationSec = elapsedDurationSec,
            avgSpeedMps = run.avgSpeedMps,
            maxSpeedMps = maxSpeedMps,
            avgPaceSecPerKm = avgPaceSecPerKm,
            calories = run.calories,
            totalDistanceMeters = run.distanceMeters,
            speedSamples = speedSamples,
            splits = splits,
            insights = insights,
        )
    }

    private fun buildSegments(points: List<RunPointEntity>): List<RunSegment> {
        if (points.size < 2) return emptyList()
        return points.zipWithNext().mapNotNull { (start, end) ->
            val durationMs = (end.tsMs - start.tsMs).coerceAtLeast(0L)
            if (durationMs <= 0L) return@mapNotNull null
            val distanceMeters = haversine(start.lat, start.lon, end.lat, end.lon)
            if (distanceMeters <= 0.0) return@mapNotNull null
            RunSegment(
                label = ((end.tsMs - points.first().tsMs) / 1000L).coerceAtLeast(0L),
                distanceMeters = distanceMeters,
                durationSec = max(1L, durationMs / 1000L),
            )
        }
    }

    private fun buildSpeedSamples(segments: List<RunSegment>): List<RunSpeedSample> {
        if (segments.isEmpty()) return emptyList()
        val bucketCount = min(6, max(1, segments.size))
        val chunkSize = max(1, (segments.size.toDouble() / bucketCount.toDouble()).roundToInt())
        return segments.chunked(chunkSize).mapIndexed { index, bucket ->
            val avgSpeed = bucket.map { it.speedMps }.average()
            RunSpeedSample(
                label = "${index + 1}",
                speedMps = avgSpeed,
            )
        }
    }

    private fun buildSplits(run: RunEntity, segments: List<RunSegment>): List<RunSplitSummary> {
        if (run.distanceMeters < 500.0 || segments.isEmpty()) return emptyList()

        val result = mutableListOf<RunSplitSummary>()
        var currentSplitDistance = 0.0
        var currentSplitDurationSec = 0.0
        var splitStartDistance = 0.0
        val splitSizeMeters = 1_000.0

        segments.forEach { segment ->
            var remainingSegmentDistance = segment.distanceMeters
            var remainingSegmentDurationSec = segment.durationSec.toDouble()

            while (remainingSegmentDistance > 0.0) {
                val distanceNeeded = splitSizeMeters - currentSplitDistance
                val distanceToConsume = min(distanceNeeded, remainingSegmentDistance)
                val proportion = distanceToConsume / remainingSegmentDistance
                val durationToConsume = remainingSegmentDurationSec * proportion

                currentSplitDistance += distanceToConsume
                currentSplitDurationSec += durationToConsume
                remainingSegmentDistance -= distanceToConsume
                remainingSegmentDurationSec -= durationToConsume

                if (currentSplitDistance >= splitSizeMeters - 0.001) {
                    result += RunSplitSummary(
                        index = result.size + 1,
                        distanceMeters = currentSplitDistance,
                        durationSec = currentSplitDurationSec.roundToInt().toLong().coerceAtLeast(1L),
                        paceSecPerKm = currentSplitDurationSec / (currentSplitDistance / 1_000.0),
                    )
                    splitStartDistance += currentSplitDistance
                    currentSplitDistance = 0.0
                    currentSplitDurationSec = 0.0
                }
            }
        }

        val remainingDistance = run.distanceMeters - splitStartDistance
        if (remainingDistance > 50.0 && currentSplitDistance > 0.0 && currentSplitDurationSec > 0.0) {
            result += RunSplitSummary(
                index = result.size + 1,
                distanceMeters = currentSplitDistance,
                durationSec = currentSplitDurationSec.roundToInt().toLong().coerceAtLeast(1L),
                paceSecPerKm = currentSplitDurationSec / (currentSplitDistance / 1_000.0),
            )
        }

        return result
    }

    private fun buildInsights(
        run: RunEntity,
        elapsedDurationSec: Long,
        maxSpeedMps: Double,
        splits: List<RunSplitSummary>,
    ): List<String> {
        val insights = mutableListOf<String>()
        val pauseSeconds = (elapsedDurationSec - run.durationSec).coerceAtLeast(0L)
        if (pauseSeconds >= 30L) {
            insights += "Detected about ${pauseSeconds}s paused or idle outside moving time."
        }
        if (splits.size >= 2) {
            val firstFull = splits.first()
            val last = splits.last()
            if (last.distanceMeters >= 800.0 && last.paceSecPerKm + 10 < firstFull.paceSecPerKm) {
                insights += "Strong finish: the last split was faster than the opening split."
            }
            val fastest = splits.minByOrNull { it.paceSecPerKm }
            if (fastest != null) {
                insights += "Best split: #${fastest.index} at ${formatPace(fastest.paceSecPerKm)}."
            }
            val paces = splits.map { it.paceSecPerKm }
            val avg = paces.average()
            val spread = paces.maxOrNull()?.minus(paces.minOrNull() ?: avg) ?: 0.0
            if (avg > 0 && spread / avg < 0.12) {
                insights += "Steady pacing: split pace stayed within a tight range."
            }
        }
        if (maxSpeedMps > 0.0) {
            insights += "Peak speed reached ${"%.1f".format(maxSpeedMps * 3.6)} km/h."
        }
        if (insights.isEmpty()) {
            insights += "Complete a longer run with more route points to unlock split and pacing insights."
        }
        return insights.distinct()
    }

    private data class RunSegment(
        val label: Long,
        val distanceMeters: Double,
        val durationSec: Long,
    ) {
        val speedMps: Double = distanceMeters / durationSec.toDouble().coerceAtLeast(1.0)
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radius * c
    }

    private fun formatPace(secPerKm: Double): String {
        val total = secPerKm.roundToInt().coerceAtLeast(0)
        val minutes = total / 60
        val seconds = total % 60
        return "$minutes:${seconds.toString().padStart(2, '0')} /km"
    }
}
