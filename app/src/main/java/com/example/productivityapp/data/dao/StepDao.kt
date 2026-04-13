package com.example.productivityapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.entities.StepSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<StepEntity?>

    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): StepEntity?

    @Query("SELECT * FROM steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: String, endDate: String): List<StepEntity>

    @Query("SELECT * FROM steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeBetweenDates(startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<StepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: StepEntity): Long

    @Update
    suspend fun update(step: StepEntity)

    @Query("DELETE FROM steps WHERE date = :date")
    suspend fun deleteByDate(date: String)

    // --- step_samples table operations ---
    @Query("SELECT * FROM step_samples WHERE date = :date ORDER BY tsMs ASC")
    fun observeSamplesByDate(date: String): kotlinx.coroutines.flow.Flow<List<StepSampleEntity>>

    @Query("SELECT * FROM step_samples WHERE date = :date ORDER BY tsMs ASC")
    suspend fun getSamplesByDate(date: String): List<StepSampleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSample(sample: StepSampleEntity): Long

    @Query("DELETE FROM step_samples WHERE date = :date")
    suspend fun deleteSamplesByDate(date: String)
}

