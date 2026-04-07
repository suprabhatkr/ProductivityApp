package com.example.productivityapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.productivityapp.data.entities.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY startTime DESC")
    fun observeAll(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: RunEntity): Long

    @Update
    suspend fun update(run: RunEntity)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteById(id: Long)
}

