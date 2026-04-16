package com.farouk.guardiantrack.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.farouk.guardiantrack.R
import com.farouk.guardiantrack.data.local.preferences.EncryptedPreferencesManager
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import com.farouk.guardiantrack.domain.model.IncidentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for emergency SMS sending.
 *
 * IMPORTANT: When SMS Simulation Mode is active (default = ON as required by spec),
 * SMS is replaced by a local notification + console log.
 * Real SMS uses SmsManager.sendTextMessage() when simulation is disabled.
 */
@Singleton
class SmsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPreferencesManager,
    private val encryptedPrefs: EncryptedPreferencesManager
) {
    companion object {
        private const val TAG = "SmsHelper"
        private const val SMS_NOTIFICATION_ID = 100
    }

    suspend fun sendEmergencyAlert(
        incidentType: IncidentType,
        latitude: Double,
        longitude: Double
    ) {
        val isSimulation = userPrefs.smsSimulationMode.first()
        val phoneNumber = encryptedPrefs.emergencyNumber

        val message = buildAlertMessage(incidentType, latitude, longitude)

        if (isSimulation) {
            // Simulation mode: local notification + log
            sendSimulatedSms(phoneNumber, message)
        } else {
            // Real mode: actual SMS
            sendRealSms(phoneNumber, message)
        }
    }

    private fun buildAlertMessage(
        type: IncidentType,
        latitude: Double,
        longitude: Double
    ): String {
        val typeLabel = when (type) {
            IncidentType.FALL -> "CHUTE DÉTECTÉE"
            IncidentType.BATTERY -> "BATTERIE CRITIQUE"
            IncidentType.MANUAL -> "ALERTE MANUELLE"
        }
        val locationUrl = if (latitude != 0.0 || longitude != 0.0) {
            "https://maps.google.com/?q=$latitude,$longitude"
        } else {
            "Position GPS indisponible"
        }
        return "🚨 GUARDIANTRACK ALERTE: $typeLabel\n📍 Localisation: $locationUrl\n⏰ ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
    }

    private fun sendSimulatedSms(phoneNumber: String, message: String) {
        Log.i(TAG, "📱 [SMS SIMULÉ] À: $phoneNumber")
        Log.i(TAG, "📱 [SMS SIMULÉ] Message: $message")

        // Show notification as substitute
        val notification = NotificationCompat.Builder(context, "alert_channel")
            .setContentTitle("📱 SMS Simulé envoyé")
            .setContentText("Destinataire: $phoneNumber")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SMS_NOTIFICATION_ID, notification)
    }

    @Suppress("MissingPermission")
    private fun sendRealSms(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "SMS permission not granted, falling back to simulation")
            sendSimulatedSms(phoneNumber, message)
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.i(TAG, "✅ SMS réel envoyé à $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Échec envoi SMS: ${e.message}")
            sendSimulatedSms(phoneNumber, message)
        }
    }
}
