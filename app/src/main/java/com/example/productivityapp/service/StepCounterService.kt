package com.example.productivityapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.content.edit
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.pm.ServiceInfo
import com.example.productivityapp.data.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Foreground service that listens to the device step counter sensor (TYPE_STEP_COUNTER)
 * and forwards increments to the StepRepository. This skeleton handles baseline offsets
 * and runs as a foreground service with a notification.
 */
class StepCounterService : Service(), SensorEventListener {
    companion object {
        const val ACTION_START = "com.example.productivityapp.action.START_STEPS"
        const val ACTION_STOP = "com.example.productivityapp.action.STOP_STEPS"
        private const val CHANNEL_ID = "steps_service_channel"
        private const val NOTIF_ID = 1001
        private const val PREFS = "step_service_prefs"
        private const val KEY_LAST_TOTAL = "last_total"
        private const val KEY_ACTIVE_DATE = "active_date"
        private const val KEY_PENDING_STEPS = "pending_steps"
        private const val KEY_LAST_FLUSH_MS = "last_flush_ms"
        private const val FLUSH_STEP_THRESHOLD = 10
        private const val FLUSH_INTERVAL_MS = 5_000L

        fun markNewDay(context: Context, today: String) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit {
                putString(KEY_ACTIVE_DATE, today)
                putInt(KEY_PENDING_STEPS, 0)
                putLong(KEY_LAST_FLUSH_MS, 0L)
            }
        }
    }

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    @Volatile private var registrationActive = false

    @VisibleForTesting
    internal var currentDateProvider: () -> String = { LocalDate.now().format(dateFormatter) }

    @VisibleForTesting
    internal var hasStepSensorOverride: Boolean? = null

    @VisibleForTesting
    internal var registerSensorUpdatesOverride: (() -> Boolean)? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> return startForegroundServiceWithNotification()
            ACTION_STOP -> stopServiceAndCleanup()
            else -> return startForegroundServiceWithNotification()
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification(): Int {
        val sensorAvailable = hasUsableStepSensor()
        val notification = buildNotification(
            if (sensorAvailable) "Step counter running" else "Step sensor unavailable — use manual entry"
        )
        // dataSync type satisfies the API 34+ requirement of declaring a foreground service type.
        // FOREGROUND_SERVICE_DATA_SYNC is a normal (install-time) permission with no runtime grant required.
        startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        if (!sensorAvailable) {
            registrationActive = false
            return START_NOT_STICKY
        }

        try {
            registrationActive = registerSensorUpdatesOverride?.invoke() ?: stepSensor?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            } ?: false
        } catch (_: SecurityException) {
            registrationActive = false
            startForeground(NOTIF_ID, buildNotification("Activity recognition permission required"), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun stopServiceAndCleanup() {
        flushPendingSteps()
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Throwable) {
            // ignore
        }
        registrationActive = false
        stopForegroundCompat()
        stopSelf()
    }

    override fun onDestroy() {
        flushPendingSteps()
        super.onDestroy()
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Throwable) {
        }
        registrationActive = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        handleStepCounterReading(event.values[0].toLong())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step Counter",
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
        val stopIntent = Intent(this, StepCounterService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Counter")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .build()
    }

    @VisibleForTesting
    internal fun buildNotificationForTest(text: String): Notification = buildNotification(text)

    @VisibleForTesting
    internal fun isRegistrationActiveForTest(): Boolean = registrationActive

    @VisibleForTesting
    internal fun handleStepCounterReading(totalSinceBoot: Long) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val today = currentDateProvider()
        val storedDate = prefs.getString(KEY_ACTIVE_DATE, null)
        val lastTotal = prefs.getLong(KEY_LAST_TOTAL, -1L)

        if (storedDate == null || lastTotal < 0L) {
            prefs.edit {
                putString(KEY_ACTIVE_DATE, today)
                putLong(KEY_LAST_TOTAL, totalSinceBoot)
                putInt(KEY_PENDING_STEPS, 0)
            }
            return
        }

        if (storedDate != today) {
            val rolloverDelta = (totalSinceBoot - lastTotal).coerceAtLeast(0L).toInt()
            prefs.edit {
                putString(KEY_ACTIVE_DATE, today)
                putLong(KEY_LAST_TOTAL, totalSinceBoot)
                putInt(KEY_PENDING_STEPS, rolloverDelta)
            }
            maybeFlushPendingSteps(today, prefs)
            return
        }

        val delta = (totalSinceBoot - lastTotal).coerceAtLeast(0L).toInt()
        if (delta <= 0) return

        val pending = prefs.getInt(KEY_PENDING_STEPS, 0) + delta
        prefs.edit {
            putLong(KEY_LAST_TOTAL, totalSinceBoot)
            putInt(KEY_PENDING_STEPS, pending)
        }

        maybeFlushPendingSteps(today, prefs)
    }

    private fun maybeFlushPendingSteps(today: String, prefs: SharedPreferences) {
        val pending = prefs.getInt(KEY_PENDING_STEPS, 0)
        val now = System.currentTimeMillis()
        val lastFlush = prefs.getLong(KEY_LAST_FLUSH_MS, 0L)
        if (pending > 0 && lastFlush == 0L) {
            prefs.edit { putLong(KEY_LAST_FLUSH_MS, now) }
            if (pending < FLUSH_STEP_THRESHOLD) return
        }
        val shouldFlush = pending >= FLUSH_STEP_THRESHOLD || (pending > 0 && now - lastFlush >= FLUSH_INTERVAL_MS)
        if (!shouldFlush) return

        prefs.edit {
            putInt(KEY_PENDING_STEPS, 0)
            putLong(KEY_LAST_FLUSH_MS, now)
        }
        serviceScope.launch {
            try {
                val repo = RepositoryProvider.provideStepRepository(applicationContext)
                repo.incrementSteps(today, pending, "sensor")
            } catch (_: Throwable) {
                // keep the service resilient; pending already dropped intentionally to avoid replay storms
            }
        }
    }

    private fun flushPendingSteps() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val pending = prefs.getInt(KEY_PENDING_STEPS, 0)
        if (pending <= 0) return

        val today = prefs.getString(KEY_ACTIVE_DATE, currentDateProvider()) ?: currentDateProvider()
        prefs.edit {
            putInt(KEY_PENDING_STEPS, 0)
            putLong(KEY_LAST_FLUSH_MS, System.currentTimeMillis())
        }
        serviceScope.launch {
            try {
                val repo = RepositoryProvider.provideStepRepository(applicationContext)
                repo.incrementSteps(today, pending, "sensor")
            } catch (_: Throwable) {
            }
        }
    }

    private fun stopForegroundCompat() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: NoSuchMethodError) {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun hasUsableStepSensor(): Boolean = hasStepSensorOverride ?: (stepSensor != null)
}
