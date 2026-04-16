package com.farouk.guardiantrack.data.mapper

import com.farouk.guardiantrack.data.local.entity.EmergencyContactEntity
import com.farouk.guardiantrack.data.local.entity.IncidentEntity
import com.farouk.guardiantrack.data.remote.dto.IncidentDto
import com.farouk.guardiantrack.domain.model.EmergencyContact
import com.farouk.guardiantrack.domain.model.Incident
import com.farouk.guardiantrack.domain.model.IncidentType

/**
 * Mappers between Entity ↔ Domain ↔ DTO layers.
 * This separation provides value when:
 * - Room schema changes don't affect UI
 * - API response format differs from local storage
 * - We need to transform/enrich data between layers
 */

// ============ Incident Mappers ============

fun IncidentEntity.toDomain(): Incident = Incident(
    id = id,
    userId = userId,
    timestamp = timestamp,
    type = try { IncidentType.valueOf(type) } catch (_: Exception) { IncidentType.MANUAL },
    latitude = latitude,
    longitude = longitude,
    isSynced = isSynced
)

fun Incident.toEntity(): IncidentEntity = IncidentEntity(
    id = id,
    userId = userId,
    timestamp = timestamp,
    type = type.name,
    latitude = latitude,
    longitude = longitude,
    isSynced = isSynced
)

fun IncidentEntity.toDto(deviceId: String = ""): IncidentDto = IncidentDto(
    timestamp = timestamp,
    type = type,
    latitude = latitude,
    longitude = longitude,
    deviceId = deviceId
)

fun IncidentDto.toEntity(userId: Long = -1L): IncidentEntity = IncidentEntity(
    timestamp = timestamp,
    type = type,
    latitude = latitude,
    longitude = longitude,
    userId = userId,
    isSynced = true
)

// ============ EmergencyContact Mappers ============

fun EmergencyContactEntity.toDomain(): EmergencyContact = EmergencyContact(
    id = id,
    name = name,
    phoneNumber = phoneNumber
)

fun EmergencyContact.toEntity(): EmergencyContactEntity = EmergencyContactEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber
)
