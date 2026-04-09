package com.example.productivityapp.run

import com.example.productivityapp.util.PolylineUtils

/**
 * Small helper utilities to support replaying encoded polylines.
 * These helpers are platform-agnostic; actual map rendering (OSMdroid) is done in UI code.
 */
object RunReplayHelper {
	data class Point(val lat: Double, val lon: Double, val tsMs: Long = 0L)

	fun decodeEncodedPolyline(encoded: String): List<Point> {
		if (encoded.isBlank()) return emptyList()
		val pairs = PolylineUtils.decode(encoded)
		return pairs.map { Point(it.first, it.second, 0L) }
	}

	/**
	 * Convert stored RunPointEntity list to replay points with timestamps (if tsMs available).
	 */
	fun fromPointEntities(points: List<com.example.productivityapp.data.entities.RunPointEntity>) : List<Point> {
		return points.map { Point(it.lat, it.lon, it.tsMs) }
	}

	/**
	 * Produce a simple sequence of steps (lat, lon) for replay at roughly uniform intervals.
	 * If timestamps are available, the function preserves timing; otherwise uses a default interval.
	 */
	fun timeline(points: List<Point>, defaultIntervalMs: Long = 1000L): List<Pair<Point, Long>> {
		if (points.isEmpty()) return emptyList()
		val out = mutableListOf<Pair<Point, Long>>()
		var prevTs = points.first().tsMs
		for (p in points) {
			val ts = if (p.tsMs > 0L) p.tsMs else (prevTs + defaultIntervalMs)
			out.add(Pair(p, ts))
			prevTs = ts
		}
		return out
	}
}


