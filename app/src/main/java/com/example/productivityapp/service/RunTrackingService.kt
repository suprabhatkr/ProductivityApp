package com.example.productivityapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.Service
import com.example.productivityapp.data.RepositoryProvider
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.*
import com.example.productivityapp.util.PolylineUtils

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

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null

    private var runId: Long = -1L
    private var lastLocation: Location? = null
    private var distanceMeters: Double = 0.0
    private var startTimeMs: Long = 0L
    // in-memory list of lat/lng pairs for encoding polyline
    private val points: MutableList<Pair<Double, Double>> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        createNotificationChannel()
    }

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
        // start foreground
        startForeground(NOTIF_ID, buildNotification("Run tracking"))
        startTimeMs = System.currentTimeMillis()
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
                // ensure points list reflects any existing stored polyline (should be empty for new run)
                if (runId > 0) {
                    // nothing to decode here; runEntity is new
                }
            } catch (t: Throwable) {
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
            fusedClient.requestLocationUpdates(locationRequest, locationCallback!!, mainLooper)
        } catch (e: SecurityException) {
            // location permission missing — callers must request permission before starting service
        }
    }

    private fun pauseRun() {
        try {
            locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        } catch (t: Throwable) {
        }
    }

    private fun resumeRun() {
        locationCallback?.let {
            try {
                fusedClient.requestLocationUpdates(locationRequest, it, mainLooper)
            } catch (e: SecurityException) {
            }
        }
    }

    private fun stopRun() {
        try {
            locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        } catch (t: Throwable) {
        }

        val endTime = System.currentTimeMillis()
        val durationSec = ((endTime - startTimeMs) / 1000L).coerceAtLeast(0L)

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
            } catch (t: Throwable) {
            }
        }

        stopForeground(true)
        stopSelf()
    }

    private fun handleLocation(loc: Location) {
        // compute incremental distance
        val prev = lastLocation
        if (prev != null) {
            val d = haversine(prev.latitude, prev.longitude, loc.latitude, loc.longitude)
            if (d > 0.5) { // ignore tiny jitter < 0.5 meters
                distanceMeters += d
            }
        }
        lastLocation = loc

        // update repository with latest stats (non-blocking)
        scope.launch {
            try {
                val repo = RepositoryProvider.provideRunRepository(applicationContext)
                val existing = if (runId > 0) repo.getRunById(runId) else null
                if (existing != null) {
                    val now = System.currentTimeMillis()
                    val durationSec = ((now - startTimeMs) / 1000L).coerceAtLeast(1L)
                    val avgSpeed = distanceMeters / durationSec
                    // append to polyline as simple CSV lat;lon;ts — replace with encoded polyline in future
                    // append to in-memory points and encode as polyline
                    points.add(Pair(loc.latitude, loc.longitude))
                    val encoded = PolylineUtils.encode(points)
                    val updated = existing.copy(
                        distanceMeters = distanceMeters,
                        durationSec = durationSec,
                        avgSpeedMps = avgSpeed,
                        calories = existing.calories,
                        polyline = encoded
                    )
                    repo.updateRun(updated)
                }
            } catch (t: Throwable) {
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Run Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Notifications for run tracking"
            nm.createNotificationChannel(channel)
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
