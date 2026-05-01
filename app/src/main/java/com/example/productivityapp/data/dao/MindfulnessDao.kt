package com.example.productivityapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MindfulnessDao {
    @Query("SELECT * FROM mindfulness_sessions ORDER BY startTime DESC")
    fun observeSessions(): Flow<List<MindfulnessSessionEntity>>

    @Query("SELECT * FROM mindfulness_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): Flow<MindfulnessSessionEntity?>

    @Query("SELECT * FROM mindfulness_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): MindfulnessSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MindfulnessSessionEntity): Long

    @Update
    suspend fun updateSession(session: MindfulnessSessionEntity)

    @Query("SELECT * FROM mind_logs ORDER BY createdAt DESC")
    fun observeLogs(): Flow<List<MindLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MindLogEntity): Long
}
