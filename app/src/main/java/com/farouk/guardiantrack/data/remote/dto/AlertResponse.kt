package com.farouk.guardiantrack.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response model from the alert API endpoint.
 */
data class AlertResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("type")
    val type: String,

    @SerializedName("status")
    val status: String = "received"
)
