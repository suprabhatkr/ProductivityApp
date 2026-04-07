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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleep: SleepEntity): Long

    @Update
    suspend fun update(sleep: SleepEntity)

    @Query("DELETE FROM sleeps WHERE id = :id")
    suspend fun deleteById(id: Long)
}

