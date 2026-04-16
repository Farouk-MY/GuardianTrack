package com.farouk.guardiantrack.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for sending incidents to the remote API.
 * Separated from Room Entity so network schema can evolve independently.
 */
data class IncidentDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("type")
    val type: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("device_id")
    val deviceId: String = ""
)
