package com.farouk.guardiantrack.ui.state

import com.farouk.guardiantrack.domain.model.EmergencyContact

/**
 * Immutable UI state for the Settings screen.
 */
data class SettingsUiState(
    val sensitivityThreshold: Float = 15.0f,
    val isDarkMode: Boolean = false,
    val isSmsSimulation: Boolean = true,
    val emergencyNumber: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val isServiceEnabled: Boolean = false,
    val showAddContactDialog: Boolean = false,
    val message: String? = null
)
