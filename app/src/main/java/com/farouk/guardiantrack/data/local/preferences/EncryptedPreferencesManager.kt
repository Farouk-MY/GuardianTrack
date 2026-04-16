package com.farouk.guardiantrack.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sensitive data using EncryptedSharedPreferences (Jetpack Security).
 * Stores: emergency phone number, API key — both encrypted at rest.
 */
@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "guardian_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var emergencyNumber: String
        get() = encryptedPrefs.getString(KEY_EMERGENCY_NUMBER, DEFAULT_EMERGENCY_NUMBER) ?: DEFAULT_EMERGENCY_NUMBER
        set(value) = encryptedPrefs.edit().putString(KEY_EMERGENCY_NUMBER, value).apply()

    var apiKey: String
        get() = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_API_KEY, value).apply()

    companion object {
        private const val KEY_EMERGENCY_NUMBER = "emergency_number"
        private const val KEY_API_KEY = "api_key"
        private const val DEFAULT_EMERGENCY_NUMBER = "+21612345678"
    }
}
