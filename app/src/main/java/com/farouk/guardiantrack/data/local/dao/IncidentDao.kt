package com.farouk.guardiantrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farouk.guardiantrack.data.local.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for incidents.
 * All queries are scoped by userId so each user only sees their own incidents.
 * Uses Flow for reactive queries as required by the specification.
 */
@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Query("SELECT * FROM incidents WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllIncidents(userId: Long): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE isSynced = 0 AND userId > 0")
    suspend fun getUnsyncedIncidents(): List<IncidentEntity>

    @Query("UPDATE incidents SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncident(id: Long)

    @Query("DELETE FROM incidents WHERE userId = :userId")
    suspend fun deleteAllIncidents(userId: Long)

    @Query("SELECT * FROM incidents WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentIncidents(userId: Long, limit: Int = 3): Flow<List<IncidentEntity>>

    @Query("SELECT COUNT(*) FROM incidents WHERE userId = :userId")
    fun getIncidentCount(userId: Long): Flow<Int>
}
