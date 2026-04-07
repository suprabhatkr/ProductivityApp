package com.example.productivityapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.productivityapp.data.entities.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<StepEntity?>

    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): StepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: StepEntity): Long

    @Update
    suspend fun update(step: StepEntity)

    @Query("DELETE FROM steps WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

