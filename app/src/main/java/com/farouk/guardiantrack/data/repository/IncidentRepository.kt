package com.farouk.guardiantrack.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.farouk.guardiantrack.data.local.dao.IncidentDao
import com.farouk.guardiantrack.data.local.entity.IncidentEntity
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import com.farouk.guardiantrack.data.mapper.toDomain
import com.farouk.guardiantrack.data.remote.NetworkResult
import com.farouk.guardiantrack.domain.model.Incident
import com.farouk.guardiantrack.domain.model.IncidentType
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for incidents.
 * Combines Room (local) + Supabase (cloud) + WorkManager (deferred sync).
 * Implements offline-first strategy as required by specification.
 *
 * All operations are scoped by userId — each user only sees their own incidents.
 */
@Singleton
class IncidentRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val supabaseClient: SupabaseClient,
    private val userPrefs: UserPreferencesManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "IncidentRepository"
    }

    /**
     * Serializable DTO for Supabase Postgrest.
     * Maps to the "incidents" table in the cloud database.
     */
    @Serializable
    data class IncidentRemote(
        val id: Long,
        @SerialName("user_id")
        val userId: Long,
        val timestamp: Long,
        val type: String,
        val latitude: Double,
        val longitude: Double
    )

    /** Get the current logged-in user ID */
    private suspend fun getCurrentUserId(): Long = userPrefs.getLoggedInUserId()

    /** Reactive stream of all incidents for the current user (from Room) */
    fun getAllIncidents(userId: Long): Flow<List<Incident>> =
        incidentDao.getAllIncidents(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    /** Recent incidents for dashboard preview */
    fun getRecentIncidents(userId: Long, limit: Int = 3): Flow<List<Incident>> =
        incidentDao.getRecentIncidents(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    /** Total incident count for the current user */
    fun getIncidentCount(userId: Long): Flow<Int> = incidentDao.getIncidentCount(userId)

    /**
     * Records a new incident:
     * 1. Attach the current user's ID to the incident
     * 2. Save to Room immediately (offline-first)
     * 3. Attempt immediate sync to Supabase
     * 4. If sync fails, leave isSynced = false for WorkManager to handle later
     */
    suspend fun recordIncident(
        type: IncidentType,
        latitude: Double,
        longitude: Double
    ): NetworkResult<Long> {
        val userId = getCurrentUserId()

        val entity = IncidentEntity(
            userId = userId,
            timestamp = System.currentTimeMillis(),
            type = type.name,
            latitude = latitude,
            longitude = longitude,
            isSynced = false
        )

        // Step 1: Always save locally first (offline-first)
        val localId = incidentDao.insertIncident(entity)
        Log.d(TAG, "💾 Incident saved locally (id=$localId, type=$type, userId=$userId)")

        // Step 2: Try to sync to Supabase immediately (only if user is logged in)
        if (userId <= 0) {
            Log.d(TAG, "⏭️ No user logged in, skipping cloud sync")
            return NetworkResult.Success(localId)
        }

        return try {
            val remote = IncidentRemote(
                id = localId,
                userId = userId,
                timestamp = entity.timestamp,
                type = entity.type,
                latitude = entity.latitude,
                longitude = entity.longitude
            )
            supabaseClient.postgrest["incidents"].upsert(remote)
            incidentDao.markAsSynced(localId)
            Log.i(TAG, "☁️ Incident synced to cloud immediately (id=$localId, userId=$userId)")
            NetworkResult.Success(localId)
        } catch (e: Exception) {
            // No network — saved locally, WorkManager will sync when connected
            Log.w(TAG, "⏳ Immediate sync failed, WorkManager will retry: ${e.message}")
            NetworkResult.Success(localId) // Still success — data is safe locally
        }
    }

    /**
     * Sync all pending (unsynced) incidents to Supabase — called by WorkManager/SyncManager.
     * Uses upsert to handle potential duplicate pushes gracefully.
     * Only syncs incidents that have a valid userId.
     */
    suspend fun syncPendingIncidents(): Boolean {
        val unsynced = incidentDao.getUnsyncedIncidents()
        if (unsynced.isEmpty()) {
            Log.d(TAG, "✅ Nothing to sync")
            return true
        }

        Log.i(TAG, "📤 Syncing ${unsynced.size} incident(s) to Supabase...")
        var allSynced = true

        for (incident in unsynced) {
            try {
                val remote = IncidentRemote(
                    id = incident.id,
                    userId = incident.userId,
                    timestamp = incident.timestamp,
                    type = incident.type,
                    latitude = incident.latitude,
                    longitude = incident.longitude
                )
                supabaseClient.postgrest["incidents"].upsert(remote)
                incidentDao.markAsSynced(incident.id)
                Log.d(TAG, "  ✅ Synced incident #${incident.id} (userId=${incident.userId})")
            } catch (e: Exception) {
                Log.w(TAG, "  ⚠️ Failed to sync incident #${incident.id}: ${e.message}")
                allSynced = false
            }
        }

        Log.i(TAG, "🏁 Sync result: ${if (allSynced) "all synced" else "partial sync"}")
        return allSynced
    }

    suspend fun deleteIncident(id: Long) {
        incidentDao.deleteIncident(id)
    }

    suspend fun deleteAllIncidents() {
        val userId = getCurrentUserId()
        incidentDao.deleteAllIncidents(userId)
    }

    /**
     * Export all incidents to CSV file in Documents/ via MediaStore (Scoped Storage).
     * Returns the display name of the created file.
     * Only exports the current user's incidents.
     * Format: Date, Heure, Type, Latitude, Longitude, Statut de synchronisation
     */
    suspend fun exportIncidentsToCSV(): String {
        val userId = getCurrentUserId()
        val incidents = incidentDao.getAllIncidents(userId).first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val fileName = "GuardianTrack_Export_${dateFormat.format(Date())}.csv"

        val csvDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val csvTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val csvContent = buildString {
            appendLine("Date,Heure,Type,Latitude,Longitude,Synchronise")
            incidents.forEach { incident ->
                val date = Date(incident.timestamp)
                appendLine(
                    "${csvDateFormat.format(date)}," +
                    "${csvTimeFormat.format(date)}," +
                    "${incident.type}," +
                    "${incident.latitude}," +
                    "${incident.longitude}," +
                    if (incident.isSynced) "Oui" else "Non"
                )
            }
        }.toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("Impossible de créer le fichier")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
            } ?: throw Exception("Impossible d'écrire dans le fichier")
        } else {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null) {
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val file = java.io.File(downloadDir, fileName)
                file.outputStream().use { outputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            } else {
                throw Exception("Stockage externe non disponible")
            }
        }

        return fileName
    }
}
