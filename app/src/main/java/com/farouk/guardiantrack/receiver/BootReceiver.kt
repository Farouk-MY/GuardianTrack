package com.farouk.guardiantrack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.farouk.guardiantrack.service.SurveillanceService
import com.farouk.guardiantrack.worker.BootWorker

/**
 * BroadcastReceiver for ACTION_BOOT_COMPLETED.
 * Restarts the SurveillanceService after device reboot.
 *
 * Android 12+ (API 31) Constraint:
 * - Cannot start foreground services directly from BroadcastReceiver.
 * - Uses WorkManager with setExpedited() as a clean workaround.
 * - On older APIs: starts the service directly.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val BOOT_WORK_NAME = "boot_surveillance_restart"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "📱 Boot completed — restarting surveillance service")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ : Use WorkManager with setExpedited()
            // This is required because foreground service start from BroadcastReceiver
            // is restricted on API 31+.
            val bootWorkRequest = OneTimeWorkRequestBuilder<BootWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                BOOT_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                bootWorkRequest
            )
            Log.i(TAG, "Scheduled expedited BootWorker for API 31+")
        } else {
            // Pre-Android 12: Start service directly
            SurveillanceService.startService(context)
            Log.i(TAG, "Started SurveillanceService directly (API < 31)")
        }
    }
}
