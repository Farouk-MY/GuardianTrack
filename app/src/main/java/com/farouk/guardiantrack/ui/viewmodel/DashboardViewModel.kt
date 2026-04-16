package com.farouk.guardiantrack.ui.viewmodel

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import com.farouk.guardiantrack.data.repository.IncidentRepository
import com.farouk.guardiantrack.data.repository.PreferencesRepository
import com.farouk.guardiantrack.domain.model.IncidentType
import com.farouk.guardiantrack.service.SurveillanceService
import com.farouk.guardiantrack.ui.state.DashboardUiState
import com.farouk.guardiantrack.util.LocationHelper
import com.farouk.guardiantrack.util.SmsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * Survives configuration changes (rotation).
 * Exposes immutable StateFlow<DashboardUiState>.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val preferencesRepository: PreferencesRepository,
    private val userPrefs: UserPreferencesManager,
    private val locationHelper: LocationHelper,
    private val smsHelper: SmsHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var sensorReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var gpsReceiver: BroadcastReceiver? = null

    init {
        observeRecentIncidents()
        observeIncidentCount()
        observeServiceState()
        updateBatteryLevel()
        updateGpsStatus()
        checkLocationPermission()
        registerSensorReceiver()
        registerBatteryReceiver()
        registerGpsReceiver()
        autoStartServiceIfNeeded()
        observeLocation()
    }

    fun checkLocationPermission() {
        val isGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(isLocationPermissionGranted = isGranted) }
        
        // If permission just granted, try restarting location observation
        if (isGranted && !_uiState.value.hasLocation) {
            observeLocation()
        }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            // Get initial location
            val initial = locationHelper.getCurrentLocation()
            if (initial != null) {
                _uiState.update {
                    it.copy(
                        currentLatitude = initial.latitude,
                        currentLongitude = initial.longitude,
                        hasLocation = true
                    )
                }
            }
        }
        // Continuous updates
        viewModelScope.launch {
            locationHelper.getLocationUpdates(5000L).collect { location ->
                _uiState.update {
                    it.copy(
                        currentLatitude = location.latitude,
                        currentLongitude = location.longitude,
                        hasLocation = true
                    )
                }
            }
        }
    }

    private fun observeRecentIncidents() {
        viewModelScope.launch {
            val userId = userPrefs.getLoggedInUserId()
            incidentRepository.getRecentIncidents(userId, 3).collect { incidents ->
                _uiState.update { it.copy(recentIncidents = incidents) }
            }
        }
    }

    private fun observeIncidentCount() {
        viewModelScope.launch {
            val userId = userPrefs.getLoggedInUserId()
            incidentRepository.getIncidentCount(userId).collect { count ->
                _uiState.update { it.copy(totalIncidents = count) }
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            preferencesRepository.serviceEnabled.collect { enabled ->
                _uiState.update { it.copy(isServiceRunning = enabled) }
            }
        }
    }

    private fun updateBatteryLevel() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        _uiState.update { it.copy(batteryLevel = level) }
    }

    fun updateGpsStatus() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        _uiState.update { it.copy(isGpsEnabled = isEnabled) }
    }

    private fun registerGpsReceiver() {
        gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    updateGpsStatus()
                }
            }
        }
        context.registerReceiver(
            gpsReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
    }

    private fun registerSensorReceiver() {
        sensorReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { i ->
                    _uiState.update { state ->
                        state.copy(
                            sensorMagnitude = i.getFloatExtra("magnitude", 0f),
                            sensorX = i.getFloatExtra("x", 0f),
                            sensorY = i.getFloatExtra("y", 0f),
                            sensorZ = i.getFloatExtra("z", 0f)
                        )
                    }
                }
            }
        }
        context.registerReceiver(
            sensorReceiver,
            IntentFilter("com.farouk.guardiantrack.SENSOR_UPDATE"),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Registers a dynamic receiver for ACTION_BATTERY_CHANGED so the battery
     * level in the UI updates live without needing to restart the app.
     */
    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { i ->
                    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val pct = if (scale > 0) (level * 100) / scale else level
                    _uiState.update { it.copy(batteryLevel = pct) }
                }
            }
        }
        // ACTION_BATTERY_CHANGED is a sticky broadcast — registers and immediately
        // delivers the last broadcast, so we get the current level instantly.
        context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    /**
     * Auto-starts the SurveillanceService on app launch if the user had it enabled.
     * This handles the case where the process was killed but the user never disabled the service.
     * Starting an already-running service is a safe no-op.
     */
    private fun autoStartServiceIfNeeded() {
        viewModelScope.launch {
            val enabled = preferencesRepository.serviceEnabled.first()
            if (enabled) {
                SurveillanceService.startService(context)
            }
        }
    }

    fun toggleService(context: Context) {
        viewModelScope.launch {
            val currentlyRunning = _uiState.value.isServiceRunning
            if (currentlyRunning) {
                SurveillanceService.stopService(context)
                preferencesRepository.setServiceEnabled(false)
            } else {
                SurveillanceService.startService(context)
                preferencesRepository.setServiceEnabled(true)
            }
        }
    }

    fun triggerManualAlert() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val location = locationHelper.getCurrentLocation()
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            incidentRepository.recordIncident(
                type = IncidentType.MANUAL,
                latitude = lat,
                longitude = lng
            )

            smsHelper.sendEmergencyAlert(
                incidentType = IncidentType.MANUAL,
                latitude = lat,
                longitude = lng
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    alertMessage = "Alerte manuelle envoyée !"
                )
            }

            updateBatteryLevel()
            updateGpsStatus()
        }
    }

    fun clearAlertMessage() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    fun simulateLowBattery() {
        // Send our custom intent that BatteryReceiver is now listening for
        val intent = Intent(context, com.farouk.guardiantrack.receiver.BatteryReceiver::class.java).apply {
            action = "com.farouk.guardiantrack.SIMULATE_BATTERY_LOW"
        }
        context.sendBroadcast(intent)
        
        _uiState.update { it.copy(alertMessage = "Simulation : Batterie Critique déclenchée") }
    }

    override fun onCleared() {
        sensorReceiver?.let { context.unregisterReceiver(it) }
        batteryReceiver?.let { context.unregisterReceiver(it) }
        gpsReceiver?.let { context.unregisterReceiver(it) }
        super.onCleared()
    }
}
