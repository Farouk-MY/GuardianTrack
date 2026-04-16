package com.farouk.guardiantrack.data.remote.api

import com.farouk.guardiantrack.data.remote.dto.AlertResponse
import com.farouk.guardiantrack.data.remote.dto.IncidentDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service interface for the GuardianTrack alert API.
 * Uses suspending functions for coroutine-based async calls.
 */
interface GuardianApiService {

    @POST("incidents")
    suspend fun sendAlert(@Body incident: IncidentDto): Response<AlertResponse>

    @POST("incidents")
    suspend fun syncIncident(@Body incident: IncidentDto): Response<AlertResponse>

    @GET("incidents")
    suspend fun getRemoteIncidents(): Response<List<IncidentDto>>
}
