package com.farouk.guardiantrack.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farouk.guardiantrack.domain.model.Incident
import com.farouk.guardiantrack.domain.model.IncidentType
import com.farouk.guardiantrack.ui.theme.*
import com.farouk.guardiantrack.ui.viewmodel.DashboardViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import android.graphics.ColorMatrixColorFilter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkLocationPermission()
                viewModel.updateGpsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.alertMessage) {
        uiState.alertMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAlertMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GuardianTheme.colors.meshPrimary
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fluid Holographic Background Shader
            HoloGlassAmbientBackground(isActive = uiState.isServiceRunning)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ===== TOP BAR =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GUARDIAN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = GuardianTheme.colors.textPrimary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPulse(isActive = uiState.isServiceRunning)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isServiceRunning) "Système Actif" else "En Attente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GuardianTheme.colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Profile Icon placeholder via glass circle
                    Surface(
                        shape = CircleShape,
                        color = GuardianTheme.colors.glassHigh,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.Person, null, modifier = Modifier.padding(12.dp), tint = GuardianTheme.colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
                
                // ===== LOCATION ALERTS =====
                if (!uiState.isLocationPermissionGranted || !uiState.isGpsEnabled) {
                    LocationAlertCard(
                        isPermissionGranted = uiState.isLocationPermissionGranted,
                        isGpsEnabled = uiState.isGpsEnabled,
                        onPermissionCheck = { viewModel.checkLocationPermission() },
                        onGpsCheck = { viewModel.updateGpsStatus() }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ===== HERO — Giant Liquid SOS Orb =====
                LiquidSOSOrb(
                    isLoading = uiState.isLoading,
                    onClick = { viewModel.triggerManualAlert() }
                )

                Spacer(modifier = Modifier.height(36.dp))

                // ===== SHIELD COMMAND Glass Card =====
                GlassCommandCard(
                    isActive = uiState.isServiceRunning,
                    onToggle = { viewModel.toggleService(context) }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ===== SENSOR DATA Glass Modules =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassSensorModule(
                        Modifier.weight(1f), Icons.Filled.Speed,
                        "MAGNITUDE", String.format("%.1f", uiState.sensorMagnitude), "m/s²",
                        if (uiState.sensorMagnitude > 12f) GuardianTheme.colors.accentDanger else GuardianTheme.colors.accentMain
                    )
                    GlassSensorModule(
                        Modifier.weight(1f), Icons.Filled.BatteryChargingFull,
                        "BATTERIE", "${uiState.batteryLevel}", "%",
                        when {
                            uiState.batteryLevel > 50 -> GuardianTheme.colors.accentSafe
                            uiState.batteryLevel > 20 -> GuardianTheme.colors.accentWarn
                            else -> GuardianTheme.colors.accentDanger
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ===== DEBUG: SIMULATE LOW BATTERY =====
                Button(
                    onClick = { viewModel.simulateLowBattery() },
                    colors = ButtonDefaults.buttonColors(containerColor = GuardianTheme.colors.glassHigh.copy(alpha=0.2f)),
                    shape = RoundedCornerShape(100),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = GuardianTheme.colors.accentDanger)
                    Spacer(Modifier.width(8.dp))
                    Text("Test: Simuler Batterie Critique", color = GuardianTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ===== ACCELEROMETER VISUALIZATION =====
                HoloWaveVisualization(
                    x = uiState.sensorX, y = uiState.sensorY,
                    z = uiState.sensorZ, magnitude = uiState.sensorMagnitude
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ===== LIVE MAP SECTION =====
                HoloGlassLiveMap(
                    latitude = uiState.currentLatitude,
                    longitude = uiState.currentLongitude,
                    hasLocation = uiState.hasLocation
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ===== RECENT INCIDENTS Glass Pills =====
                if (uiState.recentIncidents.isNotEmpty()) {
                    Text(
                        "ACTIVITÉ RÉCENTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GuardianTheme.colors.textMuted,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.recentIncidents) { incident ->
                            GlassIncidentPill(incident)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Extra padding for the floating dock
            }
        }
    }
}

// ====================================================================
// HOLOGLASS COMPONENTS
// ====================================================================

@Composable
private fun LocationAlertCard(
    isPermissionGranted: Boolean,
    isGpsEnabled: Boolean,
    onPermissionCheck: () -> Unit,
    onGpsCheck: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionCheck()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GuardianTheme.colors.accentWarn.copy(alpha = 0.15f),
        contentColor = GuardianTheme.colors.textPrimary
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = GuardianTheme.colors.accentWarn.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = GuardianTheme.colors.accentWarn
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (!isPermissionGranted) "Permission Requise" else "GPS Désactivé",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GuardianTheme.colors.accentWarn
                    )
                    Text(
                        text = if (!isPermissionGranted) "Autorisez la localisation pour la précision."
                        else "Activez le GPS pour partager votre position.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GuardianTheme.colors.textSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                onClick = {
                    if (!isPermissionGranted) {
                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    } else if (!isGpsEnabled) {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = GuardianTheme.colors.accentWarn.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "CORRIGER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = GuardianTheme.colors.meshPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusPulse(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        1f, if (isActive) 1.6f else 1f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "ps"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        0.8f, if (isActive) 0f else 0.8f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pa"
    )
    val dotColor = if (isActive) GuardianTheme.colors.accentSafe else GuardianTheme.colors.textMuted
    Box(contentAlignment = Alignment.Center) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .background(dotColor, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
private fun HoloGlassAmbientBackground(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val rotation1 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "r1")
    val rotation2 by infiniteTransition.animateFloat(360f, 0f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "r2")
    
    val pulse by infiniteTransition.animateFloat(
        0.8f, if (isActive) 1.2f else 0.8f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p1"
    )

    val c1 = GuardianTheme.colors.meshSecondary
    val c2 = GuardianTheme.colors.meshTertiary
    val activeColor = GuardianTheme.colors.accentMain

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 3
        
        // Primary floating orb
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c1.copy(alpha = 0.4f * pulse), Color.Transparent),
                center = Offset(cx + cos(Math.toRadians(rotation1.toDouble())).toFloat() * 400f, 
                                cy + sin(Math.toRadians(rotation1.toDouble())).toFloat() * 300f),
                radius = 600f
            )
        )
        
        // Secondary floating orb
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c2.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx + cos(Math.toRadians(rotation2.toDouble())).toFloat() * 350f, 
                                cy + sin(Math.toRadians(rotation2.toDouble())).toFloat() * 500f),
                radius = 700f
            )
        )

        // Center active glow
        if (isActive) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(activeColor.copy(alpha = 0.15f * pulse), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 800f
                )
            )
        }
    }
}

@Composable
private fun LiquidSOSOrb(onClick: () -> Unit, isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphX by infiniteTransition.animateFloat(
        0.95f, 1.05f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mx"
    )
    val morphY by infiniteTransition.animateFloat(
        1.05f, 0.95f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "my"
    )

    // Spring physics on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f), label = "scale"
    )

    val dangerColor = GuardianTheme.colors.accentDanger

    Box(
        contentAlignment = Alignment.Center, 
        modifier = Modifier
            .size(180.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.minDimension / 2.2f

            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(dangerColor.copy(alpha = 0.4f), Color.Transparent),
                    center = center, radius = r * 1.5f
                )
            )
            
            // Soft liquid morphing body
            drawCircle(
                color = dangerColor.copy(alpha = 0.6f),
                radius = r * morphX,
                center = center
            )
            drawCircle(
                color = dangerColor,
                radius = r * morphY * 0.85f,
                center = center
            )
            
            // Glass highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = r * morphX,
                center = center,
                style = Stroke(width = 6f)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
        } else {
            Text(
                "S O S", 
                fontSize = 28.sp, 
                fontWeight = FontWeight.Black,
                color = Color.White, 
                letterSpacing = 4.sp
            )
        }
    }
}

@Composable
private fun GlassCommandCard(isActive: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = GuardianTheme.colors.glassLow,
        contentColor = GuardianTheme.colors.textPrimary
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (isActive) GuardianTheme.colors.accentMain.copy(alpha = 0.1f) 
                        else GuardianTheme.colors.glassHigh,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Shield, 
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isActive) GuardianTheme.colors.accentMain else GuardianTheme.colors.textMuted
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isActive) "Surveillance Active" else "Surveillance en Mode Veille",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) GuardianTheme.colors.accentMain else GuardianTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Squishy Glass Button
            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = if (isActive) GuardianTheme.colors.glassHigh else GuardianTheme.colors.accentMain,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        null, 
                        tint = if (isActive) GuardianTheme.colors.textPrimary else GuardianTheme.colors.meshPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isActive) "Désactiver le Bouclier" else "Activer le Bouclier",
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (isActive) GuardianTheme.colors.textPrimary else GuardianTheme.colors.meshPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassSensorModule(
    modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, unit: String, accent: Color
) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(28.dp),
        color = GuardianTheme.colors.glassLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp))
                }
            }
            
            Column {
                Text(
                    label, 
                    style = MaterialTheme.typography.labelSmall,
                    color = GuardianTheme.colors.textSecondary, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value, 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black, 
                        color = GuardianTheme.colors.textPrimary
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            " $unit", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = GuardianTheme.colors.textMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoloWaveVisualization(x: Float, y: Float, z: Float, magnitude: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "accel")
    val wavePhase by infiniteTransition.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "wp"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = GuardianTheme.colors.glassLow
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MOUVEMENTS", 
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, 
                    color = GuardianTheme.colors.textSecondary, 
                    letterSpacing = 1.sp
                )
                Text(
                    "${String.format("%.1f", magnitude)} m/s²",
                    style = MaterialTheme.typography.labelLarge,
                    color = GuardianTheme.colors.textPrimary, fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(20.dp))

            val cX = GuardianTheme.colors.accentMain
            val cY = GuardianTheme.colors.accentSafe
            val cZ = GuardianTheme.colors.accentWarn

            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val w = size.width; val h = size.height / 2; val n = 80
                fun drawSmoothWave(amplitude: Float, phase: Float, color: Color) {
                    val path = Path()
                    for (i in 0..n) {
                        val px = (i.toFloat() / n) * w
                        val py = h + amplitude * 3.5f * sin((i.toFloat() / n) * 3 * Math.PI + wavePhase + phase).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(path, color, style = Stroke(4f, cap = StrokeCap.Round))
                }
                drawSmoothWave(x, 0f, cX.copy(alpha = 0.9f))
                drawSmoothWave(y, 2f, cY.copy(alpha = 0.8f))
                drawSmoothWave(z, 4f, cZ.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AxisPill("X", String.format("%.1f", x), cX)
                AxisPill("Y", String.format("%.1f", y), cY)
                AxisPill("Z", String.format("%.1f", z), cZ)
            }
        }
    }
}

@Composable
private fun AxisPill(axis: String, value: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                "$axis $value", 
                style = MaterialTheme.typography.labelSmall,
                color = GuardianTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GlassIncidentPill(incident: Incident) {
    val (icon, color) = when (incident.type) {
        IncidentType.FALL -> Icons.Filled.Warning to GuardianTheme.colors.accentWarn
        IncidentType.BATTERY -> Icons.Filled.BatteryAlert to GuardianTheme.colors.accentSafe
        IncidentType.MANUAL -> Icons.Filled.TouchApp to GuardianTheme.colors.accentDanger
    }
    val tf = SimpleDateFormat("HH:mm", Locale.getDefault())

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GuardianTheme.colors.glassLow,
        modifier = Modifier.width(160.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    incident.type.name, 
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, 
                    color = GuardianTheme.colors.textPrimary
                )
                Text(
                    tf.format(Date(incident.timestamp)),
                    style = MaterialTheme.typography.labelSmall, 
                    color = GuardianTheme.colors.textMuted
                )
            }
        }
    }
}

// ===== LIVE MAP SECTION =====

// Removed dark map style JSON because OSMDroid uses ColorMatrix filter to achieve a dark aesthetic

@Composable
private fun HoloGlassLiveMap(
    latitude: Double,
    longitude: Double,
    hasLocation: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }
    
    val geoPoint = GeoPoint(latitude, longitude)

    // Pulsing marker animation
    val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "pr"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "pa"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = GuardianTheme.colors.glassLow
    ) {
        Column {
            // ---- Glass Header ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = GuardianTheme.colors.accentMain.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.MyLocation, null,
                            tint = GuardianTheme.colors.accentMain,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "LOCALISATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GuardianTheme.colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            if (hasLocation) "En direct" else "GPS en attente...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasLocation) GuardianTheme.colors.accentSafe
                            else GuardianTheme.colors.textMuted
                        )
                    }
                }

                if (hasLocation) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GuardianTheme.colors.glassHigh
                    ) {
                        Text(
                            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GuardianTheme.colors.textMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ---- Map ----
            if (hasLocation) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    val accentMain = GuardianTheme.colors.accentMain
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                                controller.setZoom(16.0)
                                
                                // Invert colors to simulate dark mode
                                val filter = ColorMatrixColorFilter(
                                    floatArrayOf(
                                        -1f,  0f,  0f,  0f, 255f, // red
                                         0f, -1f,  0f,  0f, 255f, // green
                                         0f,  0f, -1f,  0f, 255f, // blue
                                         0f,  0f,  0f,  1f,   0f  // alpha
                                    )
                                )
                                overlayManager.tilesOverlay.setColorFilter(filter)
                            }
                        },
                        update = { mapView ->
                            mapView.controller.animateTo(geoPoint)
                            
                            // Keep only tiles (index 0)
                            mapView.overlays.removeAll { it !is org.osmdroid.views.overlay.TilesOverlay }
                            
                            // Outer pulsing circle
                            val outerCircle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(geoPoint, 60.0 * pulseRadius)
                                val pulseColor = accentMain.copy(alpha = pulseAlpha)
                                fillPaint.color = pulseColor.toArgb()
                                outlinePaint.color = accentMain.copy(alpha = 0.3f).toArgb()
                                outlinePaint.strokeWidth = 2f
                            }
                            
                            // Inner solid circle
                            val innerCircle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(geoPoint, 15.0)
                                fillPaint.color = accentMain.toArgb()
                                outlinePaint.color = android.graphics.Color.WHITE
                                outlinePaint.strokeWidth = 4f
                            }
                            
                            mapView.overlays.add(outerCircle)
                            mapView.overlays.add(innerCircle)
                            mapView.invalidate()
                        }
                    )
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(GuardianTheme.colors.glassHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = GuardianTheme.colors.accentMain,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Recherche du signal GPS...",
                            style = MaterialTheme.typography.bodySmall,
                            color = GuardianTheme.colors.textMuted
                        )
                    }
                }
            }
        }
    }
}
