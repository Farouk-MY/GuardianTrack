package com.farouk.guardiantrack.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.farouk.guardiantrack.data.repository.IncidentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic synchronization of incidents to Supabase.
 *
 * Strategy:
 * 1. **Foreground**: Runs a coroutine loop every 30 seconds while app is visible.
 *    Checks connectivity, then pushes unsynced incidents.
 * 2. **Connectivity**: Listens for network changes. Immediately syncs when
 *    internet comes back after being offline.
 * 3. **Background**: WorkManager periodic (15 min minimum) serves as a fallback
 *    when app is fully killed (configured in MainActivity).
 *
 * This gives the user near-real-time sync while the app is open,
 * instant sync on reconnection, and reliable background catchup.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidentRepository: IncidentRepository
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_INTERVAL_MS = 30_000L // 30 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var isNetworkAvailable = false

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "🌐 Network available — triggering immediate sync")
            isNetworkAvailable = true
            triggerImmediateSync()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "📡 Network lost — pausing remote sync")
            isNetworkAvailable = false
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            isNetworkAvailable = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    /**
     * Call once from Application.onCreate().
     * Binds to the process lifecycle so the 30-sec loop only runs
     * while the app is in the foreground.
     */
    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerNetworkCallback()
        // Check current connectivity state
        isNetworkAvailable = checkCurrentConnectivity()
        Log.i(TAG, "✅ SyncManager initialized (network=${if (isNetworkAvailable) "online" else "offline"})")
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        startPeriodicSync()
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background — WorkManager takes over
        stopPeriodicSync()
    }

    private fun startPeriodicSync() {
        if (syncJob?.isActive == true) return
        Log.i(TAG, "▶️ Starting 30-second sync loop (foreground)")

        syncJob = scope.launch {
            while (isActive) {
                performSync()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        Log.i(TAG, "⏹️ Stopped foreground sync loop (background mode — WorkManager active)")
    }

    private fun triggerImmediateSync() {
        scope.launch {
            performSync()
        }
    }

    private suspend fun performSync() {
        if (!isNetworkAvailable) return

        try {
            val success = incidentRepository.syncPendingIncidents()
            if (success) {
                Log.d(TAG, "✅ Periodic sync complete")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Sync error: ${e.message}")
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }
}
