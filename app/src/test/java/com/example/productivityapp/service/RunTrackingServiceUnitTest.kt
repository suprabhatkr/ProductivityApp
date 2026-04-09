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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class RunTrackingServiceUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    class TestLocationProvider : LocationProvider {
        var callback: com.google.android.gms.location.LocationCallback? = null
        var requested = false
        override fun requestLocationUpdates(request: com.google.android.gms.location.LocationRequest, callback: com.google.android.gms.location.LocationCallback) {
            this.callback = callback
            requested = true
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
    fun testServiceStartAndLocationHandling() = runBlocking {
        val controller = Robolectric.buildService(RunTrackingService::class.java).create()
        val service = controller.get()

        val provider = TestLocationProvider()
        service.setLocationProvider(provider)

        val intent = Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_START }
        // call onStartCommand directly; this will create a run record (async) and prepare handler
        service.onStartCommand(intent, 0, 0)
        assertTrue(provider.requested)

        // wait for the background insertion to complete (poll up to 2s)
        var runExists = false
        val startWait = System.currentTimeMillis()
        while (System.currentTimeMillis() - startWait < 2000) {
            val c = db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT COUNT(*) FROM runs"))
            if (c.moveToFirst()) {
                val cnt = c.getInt(0)
                c.close()
                if (cnt > 0) { runExists = true; break }
            } else {
                c.close()
            }
            Thread.sleep(100)
        }
        assertTrue("Run entry should have been created", runExists)

        // simulate a location callback by invoking private handleLocation via reflection
        val handleMethod = RunTrackingService::class.java.getDeclaredMethod("handleLocation", android.location.Location::class.java)
        handleMethod.isAccessible = true
        val now = System.currentTimeMillis()
        val loc = Location("test").apply {
            latitude = 12.34
            longitude = 56.78
            time = now
        }
        handleMethod.invoke(service, loc)

        // allow background work to complete
        Thread.sleep(200)

        // verify that a run was created and has polyline stored
        val cursor = db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT id, polyline FROM runs ORDER BY startTime DESC LIMIT 1"))
        var found = false
        if (cursor.moveToFirst()) {
            val poly = cursor.getString(1)
            if (!poly.isNullOrBlank()) found = true
        }
        cursor.close()

        assertTrue(found)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_PAUSE }, 0, 0)
        assertNull(provider.callback)
    }

    @Test
    fun testServicePausesAndResumesLocationProvider() {
        val controller = Robolectric.buildService(RunTrackingService::class.java).create()
        val service = controller.get()
        val provider = TestLocationProvider()
        service.setLocationProvider(provider)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_START }, 0, 0)
        assertTrue(provider.requested)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_PAUSE }, 0, 0)
        assertNull(provider.callback)

        service.onStartCommand(Intent(context, RunTrackingService::class.java).apply { action = RunTrackingService.ACTION_RESUME }, 0, 0)
        assertTrue(provider.requested)
    }
}


