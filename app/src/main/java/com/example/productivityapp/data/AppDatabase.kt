package com.example.productivityapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.productivityapp.data.dao.RunDao
import com.example.productivityapp.data.dao.RunPointDao
import com.example.productivityapp.data.dao.SleepDao
import com.example.productivityapp.data.dao.StepDao
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.StepEntity

@Database(
    entities = [StepEntity::class, RunEntity::class, SleepEntity::class, com.example.productivityapp.data.entities.RunPointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun runDao(): RunDao
    abstract fun sleepDao(): SleepDao
    abstract fun runPointDao(): RunPointDao
}

