package com.farouk.guardiantrack.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Helper for GPS location retrieval using FusedLocationProviderClient.
 *
 * Implements a fallback chain:
 * 1. getCurrentLocation() — fresh high-accuracy fix
 * 2. getLastLocation() — cached location (may be stale but better than nothing)
 * 3. null → sentinel (0.0, 0.0)
 *
 * Also provides a Flow for continuous location updates (used by the live map).
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_TIMEOUT_MS = 8000L
    }

    /**
     * Gets the current device location with a fallback chain.
     * Returns null only if permission is denied AND no cached location exists.
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "❌ Location permission not granted")
            return null
        }

        // Step 1: Try fresh location with timeout
        Log.d(TAG, "📍 Requesting fresh location...")
        val freshLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            requestFreshLocation()
        }

        if (freshLocation != null) {
            Log.i(TAG, "✅ Fresh location: ${freshLocation.latitude}, ${freshLocation.longitude}")
            return freshLocation
        }

        // Step 2: Fallback to last known location
        Log.w(TAG, "⚠️ Fresh location timed out, trying last known...")
        val lastLocation = getLastKnownLocation()

        if (lastLocation != null) {
            Log.i(TAG, "✅ Last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
        } else {
            Log.e(TAG, "❌ No location available at all")
        }

        return lastLocation
    }

    /**
     * Provides continuous location updates as a Flow.
     * Used by the live map on the Dashboard.
     */
    @Suppress("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 5000L): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, intervalMs
        ).setMinUpdateIntervalMillis(intervalMs / 2).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)
        Log.d(TAG, "📡 Started location updates (interval: ${intervalMs}ms)")

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
            Log.d(TAG, "📡 Stopped location updates")
        }
    }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationToken = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener { e ->
                Log.e(TAG, "getCurrentLocation failed: ${e.message}")
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationToken.cancel()
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "getLastLocation failed: ${e.message}")
                    continuation.resume(null)
                }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
