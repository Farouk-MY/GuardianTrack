package com.farouk.guardiantrack

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.farouk.guardiantrack.util.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Also configures WorkManager with HiltWorkerFactory for injecting dependencies into Workers.
 *
 * Creates notification channels at app startup so they are available for ALL components
 * (Service, BroadcastReceivers, SmsHelper) regardless of which one fires first.
 *
 * Initializes SyncManager for 30-second foreground sync + connectivity-triggered sync.
 */
@HiltAndroidApp
class GuardianTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        syncManager.initialize()
    }

    /**
     * Creates all notification channels at application startup.
     * This ensures channels exist before any component tries to post notifications.
     * Channel creation is idempotent — safe to call multiple times.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "surveillance_channel",
                "Surveillance Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GuardianTrack surveille vos mouvements en arrière-plan"
            }

            val alertChannel = NotificationChannel(
                "alert_channel",
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
}

