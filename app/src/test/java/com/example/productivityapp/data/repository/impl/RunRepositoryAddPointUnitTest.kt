package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.util.PolylineUtils
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class RunRepositoryAddPointUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomRunRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomRunRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAddLocationPointMigratesCsvAndAppends() = runTest {
        val now = System.currentTimeMillis()
        // create a run with CSV-style polyline: four floats -> two points
        val csv = "12.34;56.78;23.45;67.89"
        val run = RunEntity(
            startTime = now,
            endTime = null,
            distanceMeters = 0.0,
            durationSec = 0,
            avgSpeedMps = 0.0,
            calories = 0.0,
            polyline = csv
        )
        val id = repo.startRun(run)
        // add a new location point
        repo.addLocationPoint(id, 34.56, 78.90, now + 1000)

        val fetched = repo.getRunById(id)
        assertEquals(id, fetched?.id)
        val decoded = PolylineUtils.decode(fetched?.polyline ?: "")
        // original had 2 points, after append should have 3
        assertEquals(3, decoded.size)
        val last = decoded.last()
        assertEquals(34.56, last.first, 0.0001)
        assertEquals(78.90, last.second, 0.0001)

        // and one point row should have been stored in run_points for the appended point
        assertEquals(1, db.runPointDao().countByRunId(id))
    }
}

