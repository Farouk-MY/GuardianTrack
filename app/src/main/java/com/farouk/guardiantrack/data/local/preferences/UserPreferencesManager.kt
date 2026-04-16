package com.farouk.guardiantrack.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Extension property for DataStore instance */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_preferences")

/**
 * Manages user preferences via Jetpack DataStore.
 * Stores: sensitivity threshold, dark mode, SMS simulation mode,
 *         onboarding state, and auth session info.
 *
 * Note: Emergency number is stored in EncryptedPreferencesManager for security.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SENSITIVITY_THRESHOLD = floatPreferencesKey("sensitivity_threshold")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SMS_SIMULATION_MODE = booleanPreferencesKey("sms_simulation_mode")
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LOGGED_IN_USER_ID = longPreferencesKey("logged_in_user_id")
        val LOGGED_IN_USER_NAME = stringPreferencesKey("logged_in_user_name")
    }

    // ---- Existing preferences ----

    val sensitivityThreshold: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.SENSITIVITY_THRESHOLD] ?: 15.0f
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val smsSimulationMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SMS_SIMULATION_MODE] ?: true
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SERVICE_ENABLED] ?: false
    }

    // ---- Onboarding ----

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    // ---- Auth session ----

    val loggedInUserName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOGGED_IN_USER_NAME] ?: ""
    }

    val loggedInUserId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOGGED_IN_USER_ID] ?: -1L
    }

    suspend fun getLoggedInUserId(): Long =
        context.dataStore.data.first()[Keys.LOGGED_IN_USER_ID] ?: -1L

    suspend fun setLoggedInUserId(userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN_USER_ID] = userId
        }
    }

    suspend fun setLoggedInUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN_USER_NAME] = name
        }
    }

    // ---- Setters for existing preferences ----

    suspend fun setSensitivityThreshold(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SENSITIVITY_THRESHOLD] = value
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setSmsSimulationMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SMS_SIMULATION_MODE] = enabled
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVICE_ENABLED] = enabled
        }
    }
}
