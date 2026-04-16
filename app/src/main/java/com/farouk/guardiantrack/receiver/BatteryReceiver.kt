package com.farouk.guardiantrack.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.farouk.guardiantrack.R
import com.farouk.guardiantrack.data.repository.IncidentRepository
import com.farouk.guardiantrack.domain.model.IncidentType
import com.farouk.guardiantrack.util.LocationHelper
import com.farouk.guardiantrack.util.SmsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver for ACTION_BATTERY_LOW.
 * Registered statically in the manifest.
 *
 * When battery is critically low:
 * 1. Records a BATTERY incident in Room (via Hilt-injected repository)
 * 2. Gets current GPS location
 * 3. Sends emergency SMS (real or simulated depending on settings)
 * 4. Shows a last-resort notification
 *
 * Uses @AndroidEntryPoint for Hilt injection to access the same singleton
 * instances (Repository, SmsHelper, LocationHelper) as the rest of the app.
 */
@AndroidEntryPoint
class BatteryReceiver : BroadcastReceiver() {

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var smsHelper: SmsHelper
    @Inject lateinit var locationHelper: LocationHelper

    companion object {
        private const val TAG = "BatteryReceiver"
        private const val BATTERY_NOTIFICATION_ID = 3
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_LOW && intent.action != "com.farouk.guardiantrack.SIMULATE_BATTERY_LOW") return

        // Read current battery level for logging
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        Log.w(TAG, "🔋 Battery low detected! Level: $level%")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get current location
                val location = locationHelper.getCurrentLocation()
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                // 2. Record battery incident in Room (via singleton Hilt-injected repo)
                incidentRepository.recordIncident(
                    type = IncidentType.BATTERY,
                    latitude = lat,
                    longitude = lng
                )
                Log.i(TAG, "✅ Battery incident recorded (lat=$lat, lng=$lng)")

                // 3. Send emergency SMS (real or simulated based on user settings)
                smsHelper.sendEmergencyAlert(
                    incidentType = IncidentType.BATTERY,
                    latitude = lat,
                    longitude = lng
                )
                Log.i(TAG, "✅ Emergency SMS sent for battery low")

                // 4. Show local notification as visual feedback
                showBatteryNotification(context, level)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling battery low: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showBatteryNotification(context: Context, level: Int) {
        val notification = NotificationCompat.Builder(context, "alert_channel")
            .setContentTitle("🔋 Batterie Critique ($level%)")
            .setContentText("Batterie très faible ! Alerte d'urgence envoyée aux contacts. Rechargez votre appareil.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BATTERY_NOTIFICATION_ID, notification)
    }
}
