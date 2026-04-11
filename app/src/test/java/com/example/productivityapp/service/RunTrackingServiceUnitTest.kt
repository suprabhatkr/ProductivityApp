package com.example.productivityapp.service

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.DatabaseProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunTrackingServiceUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    class TestLocationProvider : LocationProvider {
        var callback: com.google.android.gms.location.LocationCallback? = null
        var requested = false
        var requestCount = 0
        var throwOnRequest = false

        override fun requestLocationUpdates(request: com.google.android.gms.location.LocationRequest, callback: com.google.android.gms.location.LocationCallback) {
            if (throwOnRequest) throw SecurityException("permission denied")
            this.callback = callback
            requested = true
            requestCount += 1
        }

        override fun removeLocationUpdates(callback: com.google.android.gms.location.LocationCallback) {
            if (this.callback === callback) this.callback = null
        }

        fun fire(lat: Double, lon: Double, timeMs: Long) {
            val loc = Location("test").apply {
                latitude = lat
                longitude = lon
                time = timeMs
            }
            callback?.onLocationResult(com.google.android.gms.location.LocationResult.create(listOf(loc)))
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // inject in-memory DB into DatabaseProvider via test hook
        DatabaseProvider.setTestInstance(db)
    }

    @After
    fun tearDown() {
        db.close()
        // clear INSTANCE
        DatabaseProvider.setTestInstance(null)
    }

    @Test
    fun serviceLifecycle_persistsLocationsAndNotificationAction() = runBlocking {
        val controller = Robolectric.buildService(RunTrackingService::class.java).create()
        val service = controller.get()

        val provider = TestLocationProvider()
        service.setLocationProvider(provider)

        val notification = service.buildNotificationForTest("Run tracking")
        assertNotNull(notification)
        assertEquals(1, notification.actions.size)
        assertEquals("Stop", notification.actions.first().title)

        val intent = Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_START }
        service.onStartCommand(intent, 0, 0)
        assertTrue(provider.requested)
        assertEquals(1, provider.requestCount)
        waitFor { latestRunId() > 0L }

        val now = System.currentTimeMillis()
        service.handleLocationForTest(Location("test").apply {
            latitude = 12.34
            longitude = 56.78
            time = now
        })
        service.handleLocationForTest(Location("test").apply {
            latitude = 12.3406
            longitude = 56.7808
            time = now + 5_000
        })

        waitFor { latestRunPointCount() >= 2 }
        assertFalse(latestPolyline().isNullOrBlank())

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_PAUSE }, 0, 0)
        assertNull(provider.callback)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_RESUME }, 0, 0)
        assertEquals(2, provider.requestCount)
        assertNotNull(provider.callback)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_STOP }, 0, 0)
        waitFor { latestEndTime() != null }
        assertNull(provider.callback)
    }

    @Test
    fun improbableJump_isIgnoredAndDoesNotPollutePolyline() = runBlocking {
        val controller = Robolectric.buildService(RunTrackingService::class.java).create()
        val service = controller.get()
        val provider = TestLocationProvider()
        service.setLocationProvider(provider)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_START }, 0, 0)
        waitFor { latestRunId() > 0L }
        val now = System.currentTimeMillis()
        service.handleLocationForTest(Location("test").apply {
            latitude = 12.34
            longitude = 56.78
            time = now
        })
        service.handleLocationForTest(Location("test").apply {
            latitude = 13.34
            longitude = 57.78
            time = now + 500
        }) // implausible jump > 50m in < 1s

        waitFor { latestRunPointCount() >= 1 }
        assertEquals(1, latestRunPointCount())
    }

    @Test
    fun permissionDeniedFromLocationProvider_doesNotCrash() {
        val controller = Robolectric.buildService(RunTrackingService::class.java).create()
        val service = controller.get()
        val provider = TestLocationProvider().apply { throwOnRequest = true }
        service.setLocationProvider(provider)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_START }, 0, 0)

        waitFor { latestRunId() > 0L }
        assertNull(provider.callback)
        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_STOP }, 0, 0)
    }

    private fun latestRunId(): Long {
        val cursor = db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT id FROM runs ORDER BY startTime DESC LIMIT 1"))
        return try {
            if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        } finally {
            cursor.close()
        }
    }

    private fun latestPolyline(): String? {
        val cursor = db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT polyline FROM runs ORDER BY startTime DESC LIMIT 1"))
        return try {
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } finally {
            cursor.close()
        }
    }

    private fun latestEndTime(): Long? {
        val cursor = db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT endTime FROM runs ORDER BY startTime DESC LIMIT 1"))
        return try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        } finally {
            cursor.close()
        }
    }

    private fun latestRunPointCount(): Int {
        val runId = latestRunId()
        if (runId <= 0L) return 0
        val cursor = db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT COUNT(*) FROM run_points WHERE runId = $runId"))
        return try {
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } finally {
            cursor.close()
        }
    }

    private fun waitFor(timeoutMs: Long = 2_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue("Condition not met within $timeoutMs ms", condition())
    }
}


