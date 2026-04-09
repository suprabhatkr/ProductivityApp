package com.example.productivityapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.productivityapp.data.entities.RunPointEntity

@Dao
interface RunPointDao {
    @Insert
    suspend fun insert(point: RunPointEntity): Long

    @Insert
    suspend fun insertAll(points: List<RunPointEntity>)

    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY tsMs ASC")
    suspend fun getByRunId(runId: Long): List<RunPointEntity>

    @Query("SELECT COUNT(*) FROM run_points WHERE runId = :runId")
    suspend fun countByRunId(runId: Long): Int
}

