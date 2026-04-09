package com.example.productivityapp.run

import org.junit.Assert.assertEquals
import org.junit.Test

class RunReplayHelperUnitTest {

    @Test
    fun timeline_preservesProvidedTimestamps() {
        val points = listOf(
            RunReplayHelper.Point(12.0, 77.0, 1000L),
            RunReplayHelper.Point(12.1, 77.1, 2500L),
            RunReplayHelper.Point(12.2, 77.2, 4000L),
        )

        val timeline = RunReplayHelper.timeline(points)

        assertEquals(3, timeline.size)
        assertEquals(1000L, timeline[0].second)
        assertEquals(2500L, timeline[1].second)
        assertEquals(4000L, timeline[2].second)
    }

    @Test
    fun timeline_appliesDefaultIntervalsWhenTimestampsMissing() {
        val points = listOf(
            RunReplayHelper.Point(12.0, 77.0, 0L),
            RunReplayHelper.Point(12.1, 77.1, 0L),
            RunReplayHelper.Point(12.2, 77.2, 0L),
        )

        val timeline = RunReplayHelper.timeline(points, defaultIntervalMs = 500L)

        assertEquals(3, timeline.size)
        assertEquals(500L, timeline[0].second)
        assertEquals(1000L, timeline[1].second)
        assertEquals(1500L, timeline[2].second)
    }
}

