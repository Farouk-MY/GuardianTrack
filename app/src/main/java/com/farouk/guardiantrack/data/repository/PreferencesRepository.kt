package com.farouk.guardiantrack.data.repository

import com.farouk.guardiantrack.data.local.preferences.EncryptedPreferencesManager
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified repository wrapping DataStore + EncryptedSharedPreferences.
 * Provides a single API for all user preference access.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val userPrefs: UserPreferencesManager,
    private val encryptedPrefs: EncryptedPreferencesManager
) {
    // ---- DataStore (reactive) ----

    val sensitivityThreshold: Flow<Float> = userPrefs.sensitivityThreshold
    val darkMode: Flow<Boolean> = userPrefs.darkMode
    val smsSimulationMode: Flow<Boolean> = userPrefs.smsSimulationMode
    val serviceEnabled: Flow<Boolean> = userPrefs.serviceEnabled

    suspend fun setSensitivityThreshold(value: Float) = userPrefs.setSensitivityThreshold(value)
    suspend fun setDarkMode(enabled: Boolean) = userPrefs.setDarkMode(enabled)
    suspend fun setSmsSimulationMode(enabled: Boolean) = userPrefs.setSmsSimulationMode(enabled)
    suspend fun setServiceEnabled(enabled: Boolean) = userPrefs.setServiceEnabled(enabled)

    // ---- Encrypted (non-reactive, synchronous) ----

    var emergencyNumber: String
        get() = encryptedPrefs.emergencyNumber
        set(value) { encryptedPrefs.emergencyNumber = value }

    var apiKey: String
        get() = encryptedPrefs.apiKey
        set(value) { encryptedPrefs.apiKey = value }
}
