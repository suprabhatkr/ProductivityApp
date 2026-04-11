package com.example.productivityapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PolylineUtilsTest {

    @Test
    fun encodeMatchesKnownFixture() {
        val points = listOf(
            38.5 to -120.2,
            40.7 to -120.95,
            43.252 to -126.453,
        )

        val encoded = PolylineUtils.encode(points)

        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", encoded)
    }

    @Test
    fun roundTrip_preservesPointsWithinEpsilon() {
        val original = listOf(
            12.9715987 to 77.594566,
            12.9721021 to 77.5962512,
            12.9739182 to 77.5998117,
            12.9751218 to 77.6034412,
        )

        val decoded = PolylineUtils.decode(PolylineUtils.encode(original))

        assertPointListsClose(original, decoded)
    }

    @Test
    fun handlesEmptySinglePointAndBounds() {
        assertTrue(PolylineUtils.decode(PolylineUtils.encode(emptyList())).isEmpty())

        val single = listOf(-33.865143 to 151.2099)
        assertPointListsClose(single, PolylineUtils.decode(PolylineUtils.encode(single)))

        val bounds = listOf(
            -90.0 to -180.0,
            90.0 to 180.0,
            0.0 to 0.0,
        )
        assertPointListsClose(bounds, PolylineUtils.decode(PolylineUtils.encode(bounds)))
    }

    @Test
    fun longSequence_roundTripsStably() {
        val points = buildList {
            repeat(1_000) { index ->
                add((10.0 + index * 0.00011) to (20.0 - index * 0.00009))
            }
        }

        val decoded = PolylineUtils.decode(PolylineUtils.encode(points))

        assertPointListsClose(points, decoded)
    }

    private fun assertPointListsClose(
        expected: List<Pair<Double, Double>>,
        actual: List<Pair<Double, Double>>,
        epsilon: Double = 1e-5,
    ) {
        assertEquals("Point count mismatch", expected.size, actual.size)
        expected.zip(actual).forEachIndexed { index, (expectedPoint, actualPoint) ->
            assertWithin("lat[$index]", expectedPoint.first, actualPoint.first, epsilon)
            assertWithin("lon[$index]", expectedPoint.second, actualPoint.second, epsilon)
        }
    }

    private fun assertWithin(label: String, expected: Double, actual: Double, epsilon: Double) {
        assertTrue(
            "$label expected=$expected actual=$actual epsilon=$epsilon",
            abs(expected - actual) <= epsilon,
        )
    }
}

