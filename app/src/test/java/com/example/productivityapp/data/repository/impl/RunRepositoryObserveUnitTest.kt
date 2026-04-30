package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunRepositoryObserveUnitTest {
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
    fun observeLatestRun_andRunPoints_returnFirstClassData() = runTest {
        val now = System.currentTimeMillis()
        val olderId = repo.startRun(
            RunEntity(
                startTime = now - 5_000,
                endTime = now - 1_000,
                distanceMeters = 1_000.0,
                durationSec = 300,
                avgSpeedMps = 3.33,
                calories = 80.0,
                polyline = "",
            )
        )
        val latestId = repo.startRun(
            RunEntity(
                startTime = now,
                endTime = null,
                distanceMeters = 500.0,
                durationSec = 120,
                avgSpeedMps = 4.16,
                calories = 42.0,
                polyline = "",
            )
        )

        repo.addLocationPoint(RunPointEntity(runId = latestId, lat = 12.34, lon = 56.78, tsMs = now + 1_000))
        repo.addLocationPoint(RunPointEntity(runId = latestId, lat = 12.35, lon = 56.79, tsMs = now + 2_000))

        val latestRun = repo.observeLatestRun().first()
        val observedRun = repo.observeRun(latestId).first()
        val points = repo.observeRunPoints(latestId).first()

        assertEquals(latestId, latestRun?.id)
        assertEquals(latestId, observedRun?.id)
        assertEquals(2, points.size)
        assertEquals(olderId, repo.getRunById(olderId)?.id)
        assertEquals(2, repo.getRunPoints(latestId).size)
    }
}
