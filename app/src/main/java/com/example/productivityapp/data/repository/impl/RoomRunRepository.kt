package com.example.productivityapp.data.repository.impl

import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.dao.RunDao
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import com.example.productivityapp.data.repository.RunRepository
import kotlinx.coroutines.flow.Flow

class RoomRunRepository(private val db: AppDatabase) : RunRepository {
    private val dao: RunDao = db.runDao()
    private val pointDao = db.runPointDao()

    override fun observeRuns(): Flow<List<RunEntity>> = dao.observeAll()

    override fun observeLatestRun(): Flow<RunEntity?> = dao.observeLatest()

    override fun observeRun(id: Long): Flow<RunEntity?> = dao.observeById(id)

    override fun observeRunPoints(runId: Long): Flow<List<RunPointEntity>> = pointDao.observeByRunId(runId)

    override suspend fun getRunById(id: Long): RunEntity? = dao.getById(id)

    override suspend fun getRunPoints(runId: Long): List<RunPointEntity> = pointDao.getByRunId(runId)

    override suspend fun startRun(run: RunEntity): Long = dao.insert(run)

    override suspend fun updateRun(run: RunEntity) { dao.update(run) }

    override suspend fun finishRun(runId: Long) {
        // No-op here; caller should update run endTime/duration and call updateRun
    }

    override suspend fun addLocationPoint(point: RunPointEntity) {
        // Fetch existing run
        val existing = dao.getById(point.runId) ?: return

        // Decode existing polyline (if encoded) or migrate CSV-style points
        val points: MutableList<Pair<Double, Double>> = mutableListOf()
        val poly = existing.polyline
        if (poly.isNotBlank()) {
            if (looksLikeEncodedPolyline(poly)) {
                points.addAll(com.example.productivityapp.util.PolylineUtils.decode(poly))
            } else {
                // attempt to parse CSV-style coordinates and convert
                points.addAll(migrateCsvPoints(poly))
            }
        }

        // append new point
        points.add(Pair(point.lat, point.lon))

        // encode and persist atomically
        val encoded = com.example.productivityapp.util.PolylineUtils.encode(points)
        val updated = existing.copy(polyline = encoded)
        dao.update(updated)

        // also persist individual point into run_points table for future queries
        pointDao.insert(point)
    }

    private fun looksLikeEncodedPolyline(s: String): Boolean {
        // Encoded polyline usually contains characters in a restricted ASCII range and not commas/semicolons
        if (s.contains(',') || s.contains(';') || s.matches(Regex("^[0-9.,;\\s]+$"))) return false
        return true
    }

    private fun migrateCsvPoints(s: String): List<Pair<Double, Double>> {
        val floats = Regex("-?\\d+\\.\\d+").findAll(s).map { it.value.toDouble() }.toList()
        val pairs = mutableListOf<Pair<Double, Double>>()
        var i = 0
        while (i + 1 < floats.size) {
            pairs.add(Pair(floats[i], floats[i + 1]))
            i += 2
        }
        return pairs
    }
}
