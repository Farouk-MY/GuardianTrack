package com.farouk.guardiantrack.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.farouk.guardiantrack.service.SurveillanceService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.core.app.NotificationCompat
import com.farouk.guardiantrack.R

/**
 * Expedited WorkManager worker for restarting SurveillanceService after boot.
 * Used on Android 12+ (API 31) where foreground services cannot be started
 * directly from a BroadcastReceiver.
 */
@HiltWorker
class BootWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            SurveillanceService.startService(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, SurveillanceService.CHANNEL_ID)
            .setContentTitle("GuardianTrack")
            .setContentText("Redémarrage du service de surveillance...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                SurveillanceService.NOTIFICATION_ID + 10,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(SurveillanceService.NOTIFICATION_ID + 10, notification)
        }
    }
}
