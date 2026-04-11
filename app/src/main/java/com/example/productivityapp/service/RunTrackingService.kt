package com.example.productivityapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import android.app.Service
import com.example.productivityapp.data.RepositoryProvider
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Foreground service skeleton for run tracking. Uses FusedLocationProviderClient to collect
 * location updates, computes distance and speed, and updates the RunRepository.
 * This implementation focuses on a safe, testable skeleton — production tuning, battery
 * optimizations and detailed error handling should be added in follow-up PRs.
 */
class RunTrackingService : Service() {
    companion object {
        const val ACTION_START = "com.example.productivityapp.action.START_RUN"
        const val ACTION_PAUSE = "com.example.productivityapp.action.PAUSE_RUN"
        const val ACTION_RESUME = "com.example.productivityapp.action.RESUME_RUN"
        const val ACTION_STOP = "com.example.productivityapp.action.STOP_RUN"
        private const val CHANNEL_ID = "run_service_channel"
        private const val NOTIF_ID = 2001
    }

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var locationProvider: LocationProvider? = null
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    private var isPaused: Boolean = false
    private var elapsedBeforePauseMs: Long = 0L
    private var activeSegmentStartElapsedMs: Long = 0L

    private var runId: Long = -1L
    private var lastLocation: Location? = null
    private var distanceMeters: Double = 0.0
    private var startTimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        locationProvider = FusedLocationProviderWrapper(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        createNotificationChannel()
    }

    // Test helper to inject a fake LocationProvider (Robolectric/unit tests)
    fun setLocationProvider(provider: LocationProvider) {
        this.locationProvider = provider
    }

    @VisibleForTesting
    internal fun buildNotificationForTest(text: String): Notification = buildNotification(text)

    @VisibleForTesting
    internal fun handleLocationForTest(location: Location) = handleLocation(location)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRun()
            ACTION_PAUSE -> pauseRun()
            ACTION_RESUME -> resumeRun()
            ACTION_STOP -> stopRun()
            else -> startRun()
        }
        return START_STICKY
    }

    private fun startRun() {
        if (runId > 0L && !isPaused) return

        // start foreground
        startForeground(NOTIF_ID, buildNotification("Run tracking"))
        startTimeMs = System.currentTimeMillis()
        activeSegmentStartElapsedMs = SystemClock.elapsedRealtime()
        elapsedBeforePauseMs = 0L
        isPaused = false
        distanceMeters = 0.0
        lastLocation = null

        // create an initial run entry in DB
        scope.launch {
            try {
                val repo = RepositoryProvider.provideRunRepository(applicationContext)
                val runEntity = com.example.productivityapp.data.entities.RunEntity(
                    startTime = startTimeMs,
                    endTime = null,
                    distanceMeters = 0.0,
                    durationSec = 0,
                    avgSpeedMps = 0.0,
                    calories = 0.0,
                    polyline = ""
                )
                runId = repo.startRun(runEntity)
            } catch (_: Throwable) {
                // ignore for skeleton
            }
        }

        // request location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    handleLocation(loc)
                }
            }
        }
        try {
            locationProvider?.requestLocationUpdates(locationRequest, locationCallback!!)
        } catch (_: SecurityException) {
            // location permission missing — callers must request permission before starting service
        }
    }

    private fun pauseRun() {
        if (isPaused) return
        try {
            locationCallback?.let { locationProvider?.removeLocationUpdates(it) }
        } catch (_: Throwable) {
        }
        elapsedBeforePauseMs += (SystemClock.elapsedRealtime() - activeSegmentStartElapsedMs).coerceAtLeast(0L)
        isPaused = true
    }

    private fun resumeRun() {
        if (!isPaused) return
        activeSegmentStartElapsedMs = SystemClock.elapsedRealtime()
        locationCallback?.let {
            try {
                locationProvider?.requestLocationUpdates(locationRequest, it)
            } catch (_: SecurityException) {
            }
        }
        isPaused = false
    }

    private fun stopRun() {
        try {
            locationCallback?.let { locationProvider?.removeLocationUpdates(it) }
        } catch (_: Throwable) {
        }

        val endTime = System.currentTimeMillis()
        val totalActiveMs = elapsedBeforePauseMs + if (!isPaused) {
            (SystemClock.elapsedRealtime() - activeSegmentStartElapsedMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val durationSec = (totalActiveMs / 1000L).coerceAtLeast(0L)

        scope.launch {
            try {
                val repo = RepositoryProvider.provideRunRepository(applicationContext)
                val existing = if (runId > 0) repo.getRunById(runId) else null
                if (existing != null) {
                    val updated = existing.copy(
                        endTime = endTime,
                        distanceMeters = distanceMeters,
                        durationSec = durationSec,
                        avgSpeedMps = if (durationSec > 0) distanceMeters / durationSec else 0.0,
                        calories = existing.calories,
                        polyline = existing.polyline
                    )
                    repo.updateRun(updated)
                }
            } catch (_: Throwable) {
            }
        }

        stopForegroundCompat()
        stopSelf()
    }

    override fun onDestroy() {
        try {
            locationCallback?.let { locationProvider?.removeLocationUpdates(it) }
        } catch (_: Throwable) {
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun handleLocation(loc: Location) {
        if (isPaused) return

        val prev = lastLocation
        val nowMs = loc.time.takeIf { it > 0L } ?: System.currentTimeMillis()

        var accept = true
        if (prev != null) {
            val d = haversine(prev.latitude, prev.longitude, loc.latitude, loc.longitude)
            val prevTime = prev.time.takeIf { it > 0L } ?: startTimeMs
            val dtSec = ((nowMs - prevTime) / 1000.0).coerceAtLeast(0.001)
            val speed = if (dtSec > 0) d / dtSec else Double.POSITIVE_INFINITY

            // filter improbably large jumps: >50m in <1s unless speed plausible (<15 m/s)
            if (d > 50.0 && dtSec < 1.0 && speed > 15.0) {
                accept = false
            }

            // ignore tiny jitter
            if (d < 0.5) accept = false

            if (accept) distanceMeters += d
        }
        if (accept) lastLocation = loc

        if (!accept) return

        // persist the new location point via repository helper
        scope.launch {
            try {
                val repo = RepositoryProvider.provideRunRepository(applicationContext)
                if (runId > 0) {
                    repo.addLocationPoint(runId, loc.latitude, loc.longitude, nowMs)

                    // update distance/duration/speed on the run record as well
                    val existing = repo.getRunById(runId)
                    if (existing != null) {
                        val totalActiveMs = elapsedBeforePauseMs +
                            (SystemClock.elapsedRealtime() - activeSegmentStartElapsedMs).coerceAtLeast(0L)
                        val durationSec = (totalActiveMs / 1000L).coerceAtLeast(1L)
                        val avgSpeed = distanceMeters / durationSec
                        val updated = existing.copy(
                            distanceMeters = distanceMeters,
                            durationSec = durationSec,
                            avgSpeedMps = avgSpeed
                        )
                        repo.updateRun(updated)
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Run Tracker",
            NotificationManager.IMPORTANCE_LOW
        )
        try {
            nm.createNotificationChannel(channel)
        } catch (_: NoSuchMethodError) {
            // Robolectric / older runtime stubs may not expose this newer API path.
        } catch (_: RuntimeException) {
            // Keep unit-test environments resilient when notification channel APIs are stubbed.
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, RunTrackingService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Run Tracker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .build()
    }

    private fun stopForegroundCompat() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: NoSuchMethodError) {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    // Haversine distance in meters
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // earth radius meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
