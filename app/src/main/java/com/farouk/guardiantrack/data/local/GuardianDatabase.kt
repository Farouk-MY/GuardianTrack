package com.farouk.guardiantrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.farouk.guardiantrack.data.local.dao.EmergencyContactDao
import com.farouk.guardiantrack.data.local.dao.IncidentDao
import com.farouk.guardiantrack.data.local.dao.UserDao
import com.farouk.guardiantrack.data.local.entity.EmergencyContactEntity
import com.farouk.guardiantrack.data.local.entity.IncidentEntity
import com.farouk.guardiantrack.data.local.entity.UserEntity

/**
 * Room Database for GuardianTrack.
 * Contains incidents, emergency contacts, and users tables.
 */
@Database(
    entities = [IncidentEntity::class, EmergencyContactEntity::class, UserEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "guardian_track_db"
    }
}
