package com.farouk.guardiantrack.data.repository

import android.util.Log
import com.farouk.guardiantrack.data.local.dao.IncidentDao
import com.farouk.guardiantrack.data.local.dao.UserDao
import com.farouk.guardiantrack.data.local.entity.UserEntity
import com.farouk.guardiantrack.data.local.preferences.EncryptedPreferencesManager
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication repository with cloud-first user management.
 *
 * Architecture:
 * - Sign-Up: Creates user in Supabase FIRST → gets globally unique ID → saves locally
 * - Sign-In (same device): Checks local Room database (instant, offline)
 * - Sign-In (new device): Falls back to Supabase → verifies → downloads user + incidents
 * - Passwords: SHA-256 with random salt (never stored in plain text)
 *
 * This ensures globally unique user IDs across all devices,
 * preventing ID collisions when syncing.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val incidentDao: IncidentDao,
    private val encryptedPrefs: EncryptedPreferencesManager,
    private val userPrefs: UserPreferencesManager,
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    /**
     * Supabase DTO for the users table.
     * Fields match exactly with Supabase column names.
     */
    @Serializable
    data class UserRemote(
        val id: Long? = null,
        val name: String,
        val email: String,
        @SerialName("password_hash")
        val passwordHash: String,
        val salt: String
    )

    /**
     * Supabase DTO for pulling incidents when signing in on a new device.
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

    /**
     * Creates a new user account.
     * Requires internet — creates in Supabase first to get a globally unique ID.
     */
    suspend fun signUp(name: String, email: String, password: String): Result<Long> {
        return try {
            val cleanEmail = email.lowercase().trim()
            val cleanName = name.trim()

            // Hash password with random salt
            val salt = generateSalt()
            val passwordHash = hashPassword(password, salt)

            // Step 1: Create user in Supabase FIRST → gets unique ID
            val remoteUser = UserRemote(
                name = cleanName,
                email = cleanEmail,
                passwordHash = passwordHash,
                salt = salt
            )

            val insertedUsers = supabaseClient.postgrest["users"]
                .insert(remoteUser) {
                    select(Columns.ALL)
                }
                .decodeList<UserRemote>()

            val supabaseId = insertedUsers.firstOrNull()?.id
                ?: return Result.failure(Exception("Erreur serveur: impossible de créer le compte"))

            Log.i(TAG, "☁️ User created in Supabase (id=$supabaseId)")

            // Step 2: Save locally with the SAME Supabase ID
            val localUser = UserEntity(
                id = supabaseId,
                name = cleanName,
                email = cleanEmail,
                passwordHash = passwordHash,
                salt = salt
            )
            userDao.insertUser(localUser)
            Log.i(TAG, "💾 User saved locally (id=$supabaseId)")

            // Step 3: Save session
            saveSession(supabaseId, cleanName)

            Result.success(supabaseId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign up failed: ${e.message}")
            // Check for duplicate email
            if (e.message?.contains("duplicate key", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true) {
                Result.failure(Exception("Un compte avec cet email existe déjà"))
            } else {
                Result.failure(Exception("Connexion internet requise pour créer un compte"))
            }
        }
    }

    /**
     * Signs in with email and password.
     * 1. Check local Room first (offline-first, same device)
     * 2. If not found locally, check Supabase (new device scenario)
     * 3. If found in Supabase, download user + their incidents
     */
    suspend fun signIn(email: String, password: String): Result<Long> {
        val cleanEmail = email.lowercase().trim()

        // Step 1: Try local Room first (offline, instant)
        val localUser = userDao.getUserByEmail(cleanEmail)
        if (localUser != null) {
            val passwordHash = hashPassword(password, localUser.salt)
            if (passwordHash != localUser.passwordHash) {
                return Result.failure(Exception("Mot de passe incorrect"))
            }

            Log.i(TAG, "✅ User signed in locally (id=${localUser.id})")
            saveSession(localUser.id, localUser.name)
            return Result.success(localUser.id)
        }

        // Step 2: Not found locally → try Supabase (new device)
        return try {
            val remoteUsers = supabaseClient.postgrest["users"]
                .select(Columns.ALL) {
                    filter {
                        eq("email", cleanEmail)
                    }
                }
                .decodeList<UserRemote>()

            val remoteUser = remoteUsers.firstOrNull()
                ?: return Result.failure(Exception("Aucun compte trouvé avec cet email"))

            // Verify password
            val passwordHash = hashPassword(password, remoteUser.salt)
            if (passwordHash != remoteUser.passwordHash) {
                return Result.failure(Exception("Mot de passe incorrect"))
            }

            val userId = remoteUser.id!!
            Log.i(TAG, "☁️ User verified from Supabase (id=$userId)")

            // Step 3: Save user locally for future offline access
            val localEntity = UserEntity(
                id = userId,
                name = remoteUser.name,
                email = remoteUser.email,
                passwordHash = remoteUser.passwordHash,
                salt = remoteUser.salt
            )
            userDao.insertUser(localEntity)
            Log.i(TAG, "💾 User downloaded to local DB (id=$userId)")

            // Step 4: Pull this user's incidents from Supabase
            pullIncidentsFromCloud(userId)

            // Step 5: Save session
            saveSession(userId, remoteUser.name)

            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign in failed: ${e.message}")
            Result.failure(Exception("Connexion internet requise pour se connecter depuis un nouvel appareil"))
        }
    }

    /**
     * Downloads all incidents for a user from Supabase and saves them locally.
     * Used when signing in on a new device to restore the user's data.
     */
    private suspend fun pullIncidentsFromCloud(userId: Long) {
        try {
            val remoteIncidents = supabaseClient.postgrest["incidents"]
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<IncidentRemote>()

            Log.i(TAG, "📥 Pulling ${remoteIncidents.size} incident(s) from cloud for user $userId")

            for (remote in remoteIncidents) {
                val entity = com.farouk.guardiantrack.data.local.entity.IncidentEntity(
                    id = remote.id,
                    userId = remote.userId,
                    timestamp = remote.timestamp,
                    type = remote.type,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    isSynced = true // Already in cloud
                )
                incidentDao.insertIncident(entity)
            }

            Log.i(TAG, "✅ ${remoteIncidents.size} incident(s) restored from cloud")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to pull incidents from cloud: ${e.message}")
            // Non-fatal — user can still use the app, incidents will sync later
        }
    }

    /**
     * Signs out the current user. Clears session but keeps local data.
     */
    suspend fun signOut() {
        userPrefs.setLoggedInUserId(-1)
        userPrefs.setLoggedInUserName("")
        Log.i(TAG, "👋 User signed out")
    }

    /**
     * Returns the currently logged-in user's name, or empty string if not logged in.
     */
    suspend fun getCurrentUserName(): String {
        val userId = userPrefs.getLoggedInUserId()
        if (userId <= 0) return ""
        val user = userDao.getUserById(userId) ?: return ""
        return user.name
    }

    /**
     * Checks if a user is currently logged in.
     */
    suspend fun isLoggedIn(): Boolean {
        return userPrefs.getLoggedInUserId() > 0
    }

    private suspend fun saveSession(userId: Long, name: String) {
        userPrefs.setLoggedInUserId(userId)
        userPrefs.setLoggedInUserName(name)
    }

    // ---- Crypto utilities ----

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password"
        return md.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String = UUID.randomUUID().toString()
}
