package com.farouk.guardiantrack.domain.model

/**
 * Domain model for incidents — used by the UI layer.
 * Clean separation from Room Entity and Network DTO.
 */
data class Incident(
    val id: Long = 0,
    val userId: Long = -1L,
    val timestamp: Long,
    val type: IncidentType,
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean = false
)
