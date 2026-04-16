package com.farouk.guardiantrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import com.farouk.guardiantrack.ui.navigation.GuardianNavigation
import com.farouk.guardiantrack.ui.navigation.Screen
import com.farouk.guardiantrack.ui.theme.*
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.farouk.guardiantrack.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, granted) ->
            android.util.Log.d("MainActivity", "$permission: ${if (granted) "granted" else "denied"}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force dark system bars to match The GuardianTheme.colors.background background
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        requestRequiredPermissions()
        scheduleSyncWorker()

        setContent {
            val darkMode by userPreferencesManager.darkMode.collectAsStateWithLifecycle(
                initialValue = true
            )

            GuardianTrackTheme(darkTheme = darkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val mainScreenRoutes = setOf(Screen.Dashboard.route, Screen.History.route, Screen.Settings.route)
                val showBottomBar = currentRoute in mainScreenRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically { it } + fadeIn(tween(300)),
                            exit = slideOutVertically { it } + fadeOut(tween(300))
                        ) {
                            HoloGlassDock(currentRoute) { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()) // Allow content to stretch under the bottom bar
                    ) {
                        GuardianNavigation(
                            navController = navController,
                            isDarkMode = darkMode,
                            onToggleDarkMode = { /* Settings handles this */ }
                        )
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }

    /**
     * Schedules a periodic background sync using WorkManager.
     * Runs every 15 minutes ONLY when the device has internet connectivity.
     * Uses KEEP policy — if already enqueued, don't duplicate.
     */
    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "incident_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

// ===== HoloGlass Floating Dock Navigation =====

data class DockItem(
    val route: String, val title: String,
    val selectedIcon: ImageVector, val unselectedIcon: ImageVector
)

@Composable
fun HoloGlassDock(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        DockItem(Screen.Dashboard.route, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        DockItem(Screen.History.route, "History", Icons.Filled.History, Icons.Outlined.History),
        DockItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 32.dp),
            shape = RoundedCornerShape(100),
            color = GuardianTheme.colors.glassHigh,
            contentColor = GuardianTheme.colors.textPrimary,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    
                    // Animated Pill logic
                    val targetWidth = if (selected) 120.dp else 48.dp
                    val animatedWidth by animateDpAsState(
                        targetValue = targetWidth, 
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                    )

                    Surface(
                        onClick = { onNavigate(item.route) },
                        shape = RoundedCornerShape(100),
                        color = if (selected) GuardianTheme.colors.accentMain else androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier
                            .height(48.dp)
                            .width(animatedWidth)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (selected) GuardianTheme.colors.meshPrimary else GuardianTheme.colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            AnimatedVisibility(
                                visible = selected,
                                enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                            ) {
                                Text(
                                    text = item.title,
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = GuardianTheme.colors.meshPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}