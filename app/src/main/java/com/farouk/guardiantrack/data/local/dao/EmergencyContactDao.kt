package com.farouk.guardiantrack.data.local.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farouk.guardiantrack.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for emergency contacts.
 * Includes Cursor-based query for ContentProvider access.
 */
@Dao
interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Query("DELETE FROM emergency_contacts WHERE _id = :id")
    suspend fun deleteContact(id: Long)

    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    suspend fun getAllContactsList(): List<EmergencyContactEntity>

    /** Cursor-based query for ContentProvider */
    @Query("SELECT * FROM emergency_contacts")
    fun getAllContactsCursor(): Cursor

    /** Cursor-based query by ID for ContentProvider */
    @Query("SELECT * FROM emergency_contacts WHERE _id = :id")
    fun getContactByIdCursor(id: Long): Cursor
}
