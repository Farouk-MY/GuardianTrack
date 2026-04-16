package com.farouk.guardiantrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.farouk.guardiantrack.MainActivity
import com.farouk.guardiantrack.R
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import com.farouk.guardiantrack.data.repository.IncidentRepository
import com.farouk.guardiantrack.domain.model.IncidentType
import com.farouk.guardiantrack.util.LocationHelper
import com.farouk.guardiantrack.util.SmsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Foreground Service that monitors accelerometer data for fall detection.
 *
 * Fall Detection Algorithm (with BONUS improvements):
 * - Phase 1 (Free-fall): magnitude < 3 m/s² for > 100ms
 * - Phase 2 (Impact): magnitude > threshold (configurable) within 200ms window
 *
 * BONUS: Low-pass filter + sliding window for improved accuracy.
 */
@AndroidEntryPoint
class SurveillanceService : Service(), SensorEventListener {

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var preferencesManager: UserPreferencesManager
    @Inject lateinit var smsHelper: SmsHelper
    @Inject lateinit var locationHelper: LocationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Fall detection state
    private var isInFreeFall = false
    private var freeFallStartTime = 0L
    private var impactWindowStartTime = 0L
    private var sensitivityThreshold = 15.0f

    // BONUS: Low-pass filter for noise reduction
    private val alpha = 0.8f
    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private var isFilterInitialized = false

    // BONUS: Sliding window for magnitude history
    private val magnitudeWindow = ArrayDeque<Float>(WINDOW_SIZE)
    private val timestampWindow = ArrayDeque<Long>(WINDOW_SIZE)

    companion object {
        const val CHANNEL_ID = "surveillance_channel"
        const val NOTIFICATION_ID = 1
        const val ALERT_CHANNEL_ID = "alert_channel"
        const val ALERT_NOTIFICATION_ID = 2
        private const val FREE_FALL_THRESHOLD = 3.0f // m/s²
        private const val FREE_FALL_MIN_DURATION = 100L // ms
        private const val IMPACT_WINDOW_DURATION = 200L // ms
        private const val WINDOW_SIZE = 50 // sliding window size
        private const val TAG = "SurveillanceService"

        fun startService(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, SurveillanceService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, createPersistentNotification())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this, sensor, SensorManager.SENSOR_DELAY_GAME
            )
        }

        // Load sensitivity threshold from DataStore
        serviceScope.launch {
            preferencesManager.sensitivityThreshold.collect { threshold ->
                sensitivityThreshold = threshold
            }
        }

        Log.d(TAG, "SurveillanceService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        Log.d(TAG, "SurveillanceService stopped")
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        // BONUS: Apply low-pass filter to reduce noise
        if (!isFilterInitialized) {
            filteredX = rawX
            filteredY = rawY
            filteredZ = rawZ
            isFilterInitialized = true
        } else {
            filteredX = alpha * filteredX + (1 - alpha) * rawX
            filteredY = alpha * filteredY + (1 - alpha) * rawY
            filteredZ = alpha * filteredZ + (1 - alpha) * rawZ
        }

        val magnitude = sqrt(
            filteredX * filteredX + filteredY * filteredY + filteredZ * filteredZ
        )
        val currentTime = System.currentTimeMillis()

        // BONUS: Update sliding window
        magnitudeWindow.addLast(magnitude)
        timestampWindow.addLast(currentTime)
        while (magnitudeWindow.size > WINDOW_SIZE) {
            magnitudeWindow.removeFirst()
            timestampWindow.removeFirst()
        }

        // Broadcast current sensor data for UI
        // IMPORTANT: setPackage() makes this an explicit broadcast so it reaches
        // receivers registered with RECEIVER_NOT_EXPORTED on Android 14+
        val sensorIntent = Intent("com.farouk.guardiantrack.SENSOR_UPDATE").apply {
            setPackage(packageName)
            putExtra("magnitude", magnitude)
            putExtra("x", filteredX)
            putExtra("y", filteredY)
            putExtra("z", filteredZ)
        }
        sendBroadcast(sensorIntent)

        // ---- Fall Detection Algorithm ----
        detectFall(magnitude, currentTime)
    }

    private fun detectFall(magnitude: Float, currentTime: Long) {
        // Phase 1: Free-fall detection
        if (magnitude < FREE_FALL_THRESHOLD) {
            if (!isInFreeFall) {
                isInFreeFall = true
                freeFallStartTime = currentTime
            }
        } else {
            if (isInFreeFall) {
                val freeFallDuration = currentTime - freeFallStartTime
                if (freeFallDuration >= FREE_FALL_MIN_DURATION) {
                    // Valid free-fall detected, open impact window
                    impactWindowStartTime = currentTime

                    // Phase 2: Impact detection
                    if (magnitude > sensitivityThreshold) {
                        onFallDetected()
                    }
                }
                isInFreeFall = false
            } else if (impactWindowStartTime > 0) {
                // Within impact window
                if (currentTime - impactWindowStartTime <= IMPACT_WINDOW_DURATION) {
                    if (magnitude > sensitivityThreshold) {
                        // BONUS: Verify with sliding window (check for consistent pattern)
                        if (verifyWithSlidingWindow()) {
                            onFallDetected()
                        }
                    }
                } else {
                    // Impact window expired
                    impactWindowStartTime = 0
                }
            }
        }
    }

    /**
     * BONUS: Sliding window verification for improved accuracy.
     * Checks for the characteristic free-fall → impact pattern in recent data.
     */
    private fun verifyWithSlidingWindow(): Boolean {
        if (magnitudeWindow.size < 10) return true // Not enough data, trust basic algorithm

        val recentValues = magnitudeWindow.toList()
        val lowCount = recentValues.takeLast(20).count { it < FREE_FALL_THRESHOLD }
        val highCount = recentValues.takeLast(5).count { it > sensitivityThreshold }

        // Pattern: several low readings followed by a spike
        return lowCount >= 3 && highCount >= 1
    }

    private fun onFallDetected() {
        impactWindowStartTime = 0
        Log.w(TAG, "⚠️ FALL DETECTED!")

        serviceScope.launch {
            // Get location
            val location = locationHelper.getCurrentLocation()
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            // Record incident in Room
            incidentRepository.recordIncident(
                type = IncidentType.FALL,
                latitude = lat,
                longitude = lng
            )

            // Send SMS (or simulate)
            smsHelper.sendEmergencyAlert(
                incidentType = IncidentType.FALL,
                latitude = lat,
                longitude = lng
            )

            // Show alert notification
            showAlertNotification("Chute détectée ! Alerte envoyée aux contacts d'urgence.")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Surveillance Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GuardianTrack surveille vos mouvements en arrière-plan"
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertes d'Urgence",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications d'alerte critique"
                enableVibration(true)
                enableLights(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun createPersistentNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianTrack Actif")
            .setContentText("Surveillance des mouvements en cours...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun showAlertNotification(message: String) {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("⚠️ Alerte GuardianTrack")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for accelerometer
    }
}
