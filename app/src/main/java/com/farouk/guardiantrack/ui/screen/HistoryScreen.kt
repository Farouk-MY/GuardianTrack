package com.farouk.guardiantrack.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farouk.guardiantrack.domain.model.Incident
import com.farouk.guardiantrack.domain.model.IncidentType
import com.farouk.guardiantrack.ui.theme.*
import com.farouk.guardiantrack.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { snackbarHostState.showSnackbar(it); viewModel.clearExportResult() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Box(modifier = Modifier.fillMaxSize().background(GuardianTheme.colors.background)) {
        // Subtle ambient background
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .size(300.dp)
                .background(Brush.radialGradient(listOf(GuardianTheme.colors.meshPrimary.copy(alpha = 0.15f), Color.Transparent)), shape = CircleShape)
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 100.dp)
                .size(250.dp)
                .background(Brush.radialGradient(listOf(GuardianTheme.colors.meshSecondary.copy(alpha = 0.15f), Color.Transparent)), shape = CircleShape)
                .blur(80.dp)
        )

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            floatingActionButton = {
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = ""
                )
                FloatingActionButton(
                    onClick = { viewModel.exportToCSV() },
                    containerColor = Color.Transparent,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    modifier = Modifier.scale(scale)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GuardianTheme.colors.accentMain, GuardianTheme.colors.meshTertiary),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, GuardianTheme.colors.glassHigh, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FileDownload, "Export", tint = GuardianTheme.colors.background)
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Header
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        "History", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary, letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${uiState.incidents.size} logs recorded",
                        style = MaterialTheme.typography.bodyMedium, color = GuardianTheme.colors.textMuted
                    )
                }

                // Liquid Filters
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidFilterPill("All", uiState.filterType == null, GuardianTheme.colors.accentMain) { viewModel.setFilter(null) }
                    LiquidFilterPill("Falls", uiState.filterType == "FALL", GuardianTheme.colors.accentDanger) {
                        viewModel.setFilter(if (uiState.filterType == "FALL") null else "FALL")
                    }
                    LiquidFilterPill("Battery", uiState.filterType == "BATTERY", GuardianTheme.colors.accentWarn) {
                        viewModel.setFilter(if (uiState.filterType == "BATTERY") null else "BATTERY")
                    }
                    LiquidFilterPill("SOS", uiState.filterType == "MANUAL", GuardianTheme.colors.meshTertiary) {
                        viewModel.setFilter(if (uiState.filterType == "MANUAL") null else "MANUAL")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(Modifier.fillMaxSize()) {
                    if (uiState.incidents.isEmpty() && !uiState.isLoading) {
                        // Empty State
                        Column(
                            Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                Modifier.size(100.dp).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GuardianTheme.colors.accentSafe.copy(0.15f), Color.Transparent))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircleOutline, null,
                                    Modifier.size(48.dp), tint = GuardianTheme.colors.accentSafe
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "All Clear", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold, color = GuardianTheme.colors.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No incidents have been recorded yet. Everything is functioning normally.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GuardianTheme.colors.textMuted, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(uiState.incidents, key = { _, item -> item.id }) { index, incident ->
                                AnimatedGlassIncidentRow(index, incident) { viewModel.deleteIncident(incident.id) }
                            }
                            item { Spacer(Modifier.height(100.dp)) }
                        }
                    }

                    if (uiState.isLoading) {
                        Box(
                            Modifier.fillMaxSize().background(GuardianTheme.colors.background.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = GuardianTheme.colors.accentMain,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidFilterPill(label: String, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(targetValue = if (selected) 1.05f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "scale")
    val alpha by animateFloatAsState(targetValue = if (selected) 1f else 0.6f, label = "alpha")
    
    val containerBrush = if (selected) {
        Brush.linearGradient(listOf(accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = 0.05f)))
    } else {
        Brush.linearGradient(listOf(GuardianTheme.colors.glassLow, GuardianTheme.colors.glassLow))
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(containerBrush)
            .border(
                1.dp,
                if (selected) accentColor.copy(alpha = 0.5f) else GuardianTheme.colors.glassHigh,
                RoundedCornerShape(20.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accentColor else GuardianTheme.colors.textPrimary.copy(alpha = alpha),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AnimatedGlassIncidentRow(index: Int, incident: Incident, onDelete: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L) // Staggered entrance
        isVisible = true
    }

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f), label = "offset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300), label = "alpha"
    )

    Box(modifier = Modifier.graphicsLayer { translationY = offsetY; this.alpha = alpha }) {
        GlassIncidentCard(incident, onDelete)
    }
}

@Composable
private fun GlassIncidentCard(incident: Incident, onDelete: () -> Unit) {
    val df = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val tf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val date = Date(incident.timestamp)

    val (icon, color, typeLabel) = when (incident.type) {
        IncidentType.FALL -> Triple(Icons.Rounded.WarningAmber, GuardianTheme.colors.accentDanger, "Fall Detected")
        IncidentType.BATTERY -> Triple(Icons.Rounded.BatteryAlert, GuardianTheme.colors.accentWarn, "Low Battery")
        IncidentType.MANUAL -> Triple(Icons.Rounded.Sos, GuardianTheme.colors.meshTertiary, "SOS Triggered")
    }

    var showDelete by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(GuardianTheme.colors.glassHigh, GuardianTheme.colors.glassLow)))
            .border(1.dp, GuardianTheme.colors.glassHigh, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Glowing Icon Wrapper
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.05f))))
                            .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                    }

                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            typeLabel, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${df.format(date)}  •  ${tf.format(date)}",
                            style = MaterialTheme.typography.labelSmall, color = GuardianTheme.colors.textMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tiny Sync indicator
                    val (sc, sl) = if (incident.isSynced) GuardianTheme.colors.accentSafe to "SYNCED" else GuardianTheme.colors.accentWarn to "PENDING"
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sc.copy(alpha = 0.1f))
                            .border(1.dp, sc.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sl, style = MaterialTheme.typography.labelSmall, color = sc, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showDelete = true }, Modifier.size(32.dp)) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(GuardianTheme.colors.glassHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete", tint = GuardianTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (incident.latitude != 0.0 || incident.longitude != 0.0) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GuardianTheme.colors.glassLow.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LocationOn, null, tint = GuardianTheme.colors.accentMain.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${String.format("%.5f", incident.latitude)}, ${String.format("%.5f", incident.longitude)}",
                        style = MaterialTheme.typography.labelMedium, color = GuardianTheme.colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Log?", fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary) },
            text = { Text("This entry will be permanently removed.", color = GuardianTheme.colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDelete = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GuardianTheme.colors.accentDanger)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = { 
                TextButton(onClick = { showDelete = false }) { 
                    Text("Cancel", color = GuardianTheme.colors.textMuted) 
                } 
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = GuardianTheme.colors.glassHigh
        )
    }
}
