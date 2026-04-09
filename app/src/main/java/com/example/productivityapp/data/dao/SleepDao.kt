package com.example.productivityapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.productivityapp.data.entities.SleepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleeps WHERE date = :date ORDER BY startTimestamp DESC")
    fun observeForDate(date: String): Flow<List<SleepEntity>>

    @Query("SELECT * FROM sleeps WHERE date BETWEEN :startDate AND :endDate ORDER BY startTimestamp DESC")
    fun observeForDateRange(startDate: String, endDate: String): Flow<List<SleepEntity>>

    @Query("SELECT * FROM sleeps WHERE endTimestamp = 0 ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): SleepEntity?

    @Query("SELECT * FROM sleeps WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SleepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleep: SleepEntity): Long

    @Update
    suspend fun update(sleep: SleepEntity)

    @Query("DELETE FROM sleeps WHERE id = :id")
    suspend fun deleteById(id: Long)
}

