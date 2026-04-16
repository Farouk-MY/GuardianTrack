package com.farouk.guardiantrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for local user accounts.
 * Email is indexed and unique to prevent duplicate registrations.
 * Password is stored as a SHA-256 hash with a random salt.
 *
 * ID comes from Supabase (not auto-generated locally) to ensure
 * globally unique IDs across all devices.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis()
)
