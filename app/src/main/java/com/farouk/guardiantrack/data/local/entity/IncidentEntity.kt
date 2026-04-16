package com.farouk.guardiantrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an incident stored in the local database.
 * Separated from the domain model (Incident) and network DTO (IncidentDto).
 *
 * userId links each incident to the user who triggered it.
 * This ensures multi-user isolation: each user only sees their own incidents.
 */
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = -1L,
    val timestamp: Long,
    val type: String, // "FALL", "BATTERY", or "MANUAL"
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean = false
)
