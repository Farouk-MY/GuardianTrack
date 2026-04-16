package com.farouk.guardiantrack.ui.state

import com.farouk.guardiantrack.domain.model.Incident

/**
 * Immutable UI state for the Dashboard screen.
 */
data class DashboardUiState(
    val isServiceRunning: Boolean = false,
    val sensorMagnitude: Float = 0f,
    val sensorX: Float = 0f,
    val sensorY: Float = 0f,
    val sensorZ: Float = 0f,
    val batteryLevel: Int = 100,
    val isGpsEnabled: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val hasLocation: Boolean = false,
    val recentIncidents: List<Incident> = emptyList(),
    val totalIncidents: Int = 0,
    val isLoading: Boolean = false,
    val alertMessage: String? = null
)
