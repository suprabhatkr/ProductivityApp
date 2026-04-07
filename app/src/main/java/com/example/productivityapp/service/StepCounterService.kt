package com.example.productivityapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.Service
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
        private const val KEY_BASELINE = "baseline"
    }

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundServiceWithNotification()
            ACTION_STOP -> stopServiceAndCleanup()
            else -> startForegroundServiceWithNotification()
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification("Step counter running")
        startForeground(NOTIF_ID, notification)
        stepSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopServiceAndCleanup() {
        try {
            sensorManager.unregisterListener(this)
        } catch (t: Throwable) {
            // ignore
        }
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            sensorManager.unregisterListener(this)
        } catch (t: Throwable) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        // event.values[0] is the total steps since last reboot (float)
        val totalSinceBoot = event.values[0].toLong()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val baseline = prefs.getLong(KEY_BASELINE, -1L)

        if (baseline < 0L) {
            // first reading after service start — set baseline
            prefs.edit().putLong(KEY_BASELINE, totalSinceBoot).apply()
            return
        }

        val delta = (totalSinceBoot - baseline).toInt()
        if (delta <= 0) return

        // update baseline to current so we won't double-count next event
        prefs.edit().putLong(KEY_BASELINE, totalSinceBoot).apply()

        // forward to repository on background coroutine
        serviceScope.launch {
            try {
                val repo = RepositoryProvider.provideStepRepository(applicationContext)
                val today = LocalDate.now().format(dateFormatter)
                repo.incrementSteps(today, delta, "sensor")
            } catch (t: Throwable) {
                // swallow — service should be resilient
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Notifications for background step counting"
            nm.createNotificationChannel(channel)
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
}
