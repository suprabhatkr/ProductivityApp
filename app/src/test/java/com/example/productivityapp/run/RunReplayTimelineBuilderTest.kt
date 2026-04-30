package com.example.productivityapp.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunReplayTimelineBuilderTest {

    private val builder = RunReplayTimelineBuilder()

    @Test
    fun buildInteractiveTimeline_generatesReplayFramesWithViewportAndProgress() {
        val points = listOf(
            RunReplayHelper.Point(12.9716, 77.5946, 1_000L),
            RunReplayHelper.Point(12.9720, 77.5951, 3_000L),
            RunReplayHelper.Point(12.9790, 77.6060, 7_000L),
        )

        val frames = builder.buildInteractiveTimeline(points, durationMs = 9_000L, maxFrames = 10)

        assertEquals(3, frames.size)
        assertEquals(1, frames.first().visiblePointCount)
        assertEquals(points.size, frames.last().visiblePointCount)
        assertEquals(1f, frames.last().normalizedProgress, 0f)
        assertTrue(frames.last().distanceMeters > frames.first().distanceMeters)
        assertNotNull(frames.last().cameraViewport)
        assertTrue(frames[0].frameDurationMs > 0L)
    }

    @Test
    fun buildFixedDurationTimeline_normalizesToRequestedFrameCount() {
        val points = listOf(
            RunReplayHelper.Point(12.9716, 77.5946, 1_000L),
            RunReplayHelper.Point(12.9720, 77.5951, 2_000L),
            RunReplayHelper.Point(12.9790, 77.6060, 7_000L),
            RunReplayHelper.Point(12.9850, 77.6120, 11_000L),
        )

        val frames = builder.buildFixedDurationTimeline(points, durationMs = 2_000L, fps = 5)

        assertEquals(10, frames.size)
        assertEquals(1, frames.first().visiblePointCount)
        assertEquals(points.size, frames.last().visiblePointCount)
        assertTrue(frames.zipWithNext().all { (left, right) ->
            right.visiblePointCount >= left.visiblePointCount &&
                right.distanceMeters >= left.distanceMeters
        })
    }

    @Test
    fun buildFixedDurationTimeline_handlesLongRoutesWithoutLosingTerminalState() {
        val points = List(2_000) { index ->
            RunReplayHelper.Point(
                lat = 12.9716 + (index * 0.00005),
                lon = 77.5946 + (index * 0.00005),
                tsMs = 1_000L + index * 1_000L,
            )
        }

        val frames = builder.buildFixedDurationTimeline(points, durationMs = 4_000L, fps = 15)

        assertEquals(60, frames.size)
        assertEquals(1, frames.first().visiblePointCount)
        assertEquals(points.size, frames.last().visiblePointCount)
        assertEquals(1f, frames.last().normalizedProgress, 0f)
        assertTrue(frames.last().distanceMeters > 0.0)
        assertTrue(frames.all { it.cameraViewport != null })
    }

    @Test
    fun buildInteractiveTimeline_expandsCameraProgressivelyAcrossVisibleRoute() {
        val points = listOf(
            RunReplayHelper.Point(12.9716, 77.5946, 1_000L),
            RunReplayHelper.Point(12.9720, 77.5951, 2_000L),
            RunReplayHelper.Point(12.9755, 77.6002, 3_000L),
            RunReplayHelper.Point(12.9790, 77.6060, 4_000L),
        )

        val frames = builder.buildInteractiveTimeline(points, durationMs = 8_000L, maxFrames = 12)

        assertTrue(frames.size >= points.size)
        frames.forEachIndexed { index, frame ->
            val viewport = requireNotNull(frame.cameraViewport)
            val visiblePoints = points.take(frame.visiblePointCount)
            visiblePoints.forEach { point ->
                assertTrue(point.lat in viewport.south..viewport.north)
                assertTrue(point.lon in viewport.west..viewport.east)
            }

            if (index > 0) {
                val previous = requireNotNull(frames[index - 1].cameraViewport)
                val currentHeight = viewport.north - viewport.south
                val currentWidth = viewport.east - viewport.west
                val previousHeight = previous.north - previous.south
                val previousWidth = previous.east - previous.west
                assertTrue(currentHeight >= previousHeight)
                assertTrue(currentWidth >= previousWidth)
            }
        }
    }
}
