package com.example.productivityapp.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.productivityapp.util.PolylineUtils
import com.example.productivityapp.data.entities.RunPointEntity

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    @Volatile
    private var polylineMigrationBootstrapped: Boolean = false

    fun getInstance(context: Context): AppDatabase {
        val instance = INSTANCE ?: synchronized(this) {
            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // create run_points table
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS run_points (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, runId INTEGER NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL, tsMs INTEGER NOT NULL)"
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_run_points_runId ON run_points(runId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_run_points_runId_tsMs ON run_points(runId, tsMs)")
                }
            }

            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // create step_samples table
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS step_samples (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, tsMs INTEGER NOT NULL, delta INTEGER NOT NULL, source TEXT NOT NULL)"
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_step_samples_date ON step_samples(date)")
                }
            }

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "productivity-db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
            INSTANCE = instance
            instance
        }

        bootstrapPolylineMigration(context.applicationContext, instance)
        return instance
    }

    // Test hook: allow tests to inject an in-memory database instance
    fun setTestInstance(db: AppDatabase?) {
        INSTANCE = db
        polylineMigrationBootstrapped = db != null
    }

    /**
     * Runtime helper to migrate existing encoded or CSV-style polyline strings in `runs.polyline`
     * into the new `run_points` table. This is executed after DB is opened to allow use of
     * application code (PolylineUtils) for decoding.
     */
    fun migratePolylinesIfNeeded(context: Context) {
        val db = getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            migratePolylinesInternal(db)
        }
    }

    private fun bootstrapPolylineMigration(context: Context, db: AppDatabase) {
        if (polylineMigrationBootstrapped) return

        val shouldRun = synchronized(this) {
            if (polylineMigrationBootstrapped) {
                false
            } else {
                polylineMigrationBootstrapped = true
                true
            }
        }

        if (!shouldRun) return

        CoroutineScope(Dispatchers.IO).launch {
            migratePolylinesInternal(db)
        }
    }

    private suspend fun migratePolylinesInternal(db: AppDatabase) {
        val pointDao = db.runPointDao()
        val cursor = db.query(SimpleSQLiteQuery("SELECT id, polyline FROM runs"))
        try {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val poly = cursor.getString(1) ?: ""
                if (poly.isBlank()) continue

                val count = pointDao.countByRunId(id)
                if (count > 0) continue

                val points = if (poly.contains(',') || poly.contains(';') || poly.matches(Regex("^[0-9.,;\\s]+$"))) {
                    val floats = Regex("-?\\d+\\.\\d+").findAll(poly).map { it.value.toDouble() }.toList()
                    val pairs = mutableListOf<Pair<Double, Double>>()
                    var i = 0
                    while (i + 1 < floats.size) {
                        pairs.add(Pair(floats[i], floats[i + 1]))
                        i += 2
                    }
                    pairs
                } else {
                    PolylineUtils.decode(poly)
                }

                val entities = points.map { RunPointEntity(runId = id, lat = it.first, lon = it.second, tsMs = 0L) }
                if (entities.isNotEmpty()) pointDao.insertAll(entities)
            }
        } finally {
            cursor.close()
        }
    }
}

