package com.example.productivityapp.util

/**
 * Utility to encode/decode polylines using the Google encoded polyline algorithm.
 * Implementation adapted for Kotlin.
 */
object PolylineUtils {
    fun encode(points: List<Pair<Double, Double>>): String {
        val result = StringBuilder()
        var lastLat = 0
        var lastLng = 0

        for ((lat, lng) in points) {
            val latE5 = (lat * 1e5).roundToInt()
            val lngE5 = (lng * 1e5).roundToInt()

            var dLat = latE5 - lastLat
            var dLng = lngE5 - lastLng

            encodeSignedNumber(dLat, result)
            encodeSignedNumber(dLng, result)

            lastLat = latE5
            lastLng = lngE5
        }

        return result.toString()
    }

    fun decode(encoded: String): List<Pair<Double, Double>> {
        val len = encoded.length
        var index = 0
        var lat = 0
        var lng = 0
        val path = ArrayList<Pair<Double, Double>>()

        while (index < len) {
            val (dLat, nextIndex) = decodeNumber(encoded, index)
            index = nextIndex
            val (dLng, nextIndex2) = decodeNumber(encoded, index)
            index = nextIndex2

            lat += dLat
            lng += dLng

            path.add(Pair(lat / 1e5, lng / 1e5))
        }

        return path
    }

    private fun encodeSignedNumber(num: Int, sb: StringBuilder) {
        var sgnNum = num shl 1
        if (num < 0) sgnNum = sgnNum.inv()
        encodeNumber(sgnNum, sb)
    }

    private fun encodeNumber(num: Int, sb: StringBuilder) {
        var n = num
        while (n >= 0x20) {
            val nextValue = (0x20 or (n and 0x1f)) + 63
            sb.append(nextValue.toChar())
            n = n shr 5
        }
        n += 63
        sb.append(n.toChar())
    }

    private fun decodeNumber(encoded: String, startIndex: Int): Pair<Int, Int> {
        var index = startIndex
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)

        val dCoord = if ((result and 1) != 0) (result.inv() shr 1) else (result shr 1)
        return Pair(dCoord, index)
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}

