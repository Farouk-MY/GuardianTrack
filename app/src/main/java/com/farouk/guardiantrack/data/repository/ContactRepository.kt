package com.farouk.guardiantrack.data.repository

import com.farouk.guardiantrack.data.local.dao.EmergencyContactDao
import com.farouk.guardiantrack.data.local.entity.EmergencyContactEntity
import com.farouk.guardiantrack.data.mapper.toDomain
import com.farouk.guardiantrack.data.mapper.toEntity
import com.farouk.guardiantrack.domain.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for emergency contacts CRUD operations.
 * Backed by Room database.
 */
@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: EmergencyContactDao
) {
    fun getAllContacts(): Flow<List<EmergencyContact>> =
        contactDao.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getAllContactsList(): List<EmergencyContact> =
        contactDao.getAllContactsList().map { it.toDomain() }

    suspend fun addContact(contact: EmergencyContact): Long =
        contactDao.insertContact(contact.toEntity())

    suspend fun addContact(name: String, phoneNumber: String): Long =
        contactDao.insertContact(
            EmergencyContactEntity(name = name, phoneNumber = phoneNumber)
        )

    suspend fun deleteContact(id: Long) =
        contactDao.deleteContact(id)
}
