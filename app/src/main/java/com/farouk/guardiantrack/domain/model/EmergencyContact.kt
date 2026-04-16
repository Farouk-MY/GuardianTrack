package com.farouk.guardiantrack.domain.model

/**
 * Domain model for emergency contacts — used by the UI layer.
 */
data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String
)
