package com.farouk.guardiantrack.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farouk.guardiantrack.domain.model.EmergencyContact
import com.farouk.guardiantrack.ui.theme.*
import com.farouk.guardiantrack.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    Box(modifier = Modifier.fillMaxSize().background(GuardianTheme.colors.background)) {
        // Fluid Background Mesh
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-100).dp)
                .size(350.dp)
                .background(Brush.radialGradient(listOf(GuardianTheme.colors.meshPrimary.copy(alpha = 0.2f), Color.Transparent)), shape = CircleShape)
                .blur(100.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 100.dp, y = 50.dp)
                .size(300.dp)
                .background(Brush.radialGradient(listOf(GuardianTheme.colors.meshSecondary.copy(alpha = 0.15f), Color.Transparent)), shape = CircleShape)
                .blur(80.dp)
        )

        Scaffold(
            snackbarHost = { com.farouk.guardiantrack.ui.components.GuardianSnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HEADER
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "Settings", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary, letterSpacing = (-1).sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Configure your personal protection",
                            style = MaterialTheme.typography.bodyMedium, color = GuardianTheme.colors.textMuted
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    
                    // Profile/Status Banner
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(GuardianTheme.colors.accentMain.copy(alpha = 0.2f), GuardianTheme.colors.meshTertiary.copy(alpha = 0.05f))))
                            .border(1.dp, GuardianTheme.colors.glassHigh, RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(64.dp).clip(CircleShape).background(GuardianTheme.colors.glassHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Person, null, tint = GuardianTheme.colors.accentMain, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Guardian User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary)
                                Text("Protection Active", style = MaterialTheme.typography.labelMedium, color = GuardianTheme.colors.accentSafe)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ===== DETECTION =====
                item {
                    SettingsSection("DETECTION", Icons.Rounded.Sensors, GuardianTheme.colors.accentMain) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sensitivity Threshold", style = MaterialTheme.typography.bodyMedium, color = GuardianTheme.colors.textPrimary)
                            Text(
                                "${String.format("%.1f", uiState.sensitivityThreshold)} m/s²",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold, color = GuardianTheme.colors.accentMain
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Slider(
                            value = uiState.sensitivityThreshold,
                            onValueChange = { viewModel.setSensitivityThreshold(it) },
                            valueRange = 5f..30f, steps = 24,
                            colors = SliderDefaults.colors(
                                thumbColor = GuardianTheme.colors.accentMain,
                                activeTrackColor = GuardianTheme.colors.accentMain,
                                inactiveTrackColor = GuardianTheme.colors.accentMain.copy(alpha = 0.1f)
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("5.0", style = MaterialTheme.typography.labelSmall, color = GuardianTheme.colors.textDisabled)
                            Text("DEFAULT: 15.0", style = MaterialTheme.typography.labelSmall, color = GuardianTheme.colors.textDisabled, letterSpacing = 1.sp)
                            Text("30.0", style = MaterialTheme.typography.labelSmall, color = GuardianTheme.colors.textDisabled)
                        }
                    }
                }

                // ===== COMMUNICATION =====
                item {
                    SettingsSection("COMMUNICATION", Icons.Rounded.Sms, GuardianTheme.colors.meshTertiary) {
                        var numberInput by remember { mutableStateOf(uiState.emergencyNumber) }
                        LaunchedEffect(uiState.emergencyNumber) { numberInput = uiState.emergencyNumber }

                        OutlinedTextField(
                            value = numberInput, onValueChange = { numberInput = it },
                            label = { Text("Emergency Number", color = GuardianTheme.colors.textMuted) },
                            leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = GuardianTheme.colors.meshTertiary) },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.setEmergencyNumber(numberInput) }) {
                                    Icon(Icons.Rounded.Save, "Save", tint = GuardianTheme.colors.accentMain)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GuardianTheme.colors.accentMain,
                                unfocusedBorderColor = GuardianTheme.colors.glassHigh,
                                focusedContainerColor = GuardianTheme.colors.glassHigh.copy(alpha = 0.5f),
                                unfocusedContainerColor = GuardianTheme.colors.glassLow.copy(alpha = 0.3f),
                                cursorColor = GuardianTheme.colors.accentMain,
                                focusedTextColor = GuardianTheme.colors.textPrimary,
                                unfocusedTextColor = GuardianTheme.colors.textPrimary
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        HoloGlassToggle(
                            "SMS Simulation Mode",
                            if (uiState.isSmsSimulation) "Simulated SMS via notification" else "⚠️ Real SMS will be sent",
                            Icons.Rounded.InstallMobile,
                            uiState.isSmsSimulation,
                            { viewModel.setSmsSimulationMode(it) },
                            if (uiState.isSmsSimulation) GuardianTheme.colors.accentSafe else GuardianTheme.colors.accentWarn
                        )
                    }
                }

                // ===== CONTACTS =====
                item {
                    SettingsSection("EMERGENCY CONTACTS", Icons.Rounded.Contacts, GuardianTheme.colors.accentSafe) {
                        uiState.emergencyContacts.forEach { contact ->
                            GlassContactRow(contact) { viewModel.deleteContact(contact.id) }
                            Spacer(Modifier.height(10.dp))
                        }

                        Button(
                            onClick = { viewModel.showAddContactDialog() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GuardianTheme.colors.accentMain.copy(alpha = 0.15f),
                                contentColor = GuardianTheme.colors.accentMain
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                        ) {
                            Icon(Icons.Rounded.PersonAdd, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ADD CONTACT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // ===== APPEARANCE =====
                item {
                    SettingsSection("APPEARANCE", Icons.Rounded.Palette, GuardianTheme.colors.accentWarn) {
                        HoloGlassToggle(
                            "Dark Mode",
                            if (isDarkMode) "Dark theme active" else "Light theme active",
                            if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            isDarkMode,
                            { onToggleDarkMode(it); viewModel.setDarkMode(it) },
                            if (isDarkMode) GuardianTheme.colors.accentMain else GuardianTheme.colors.accentWarn
                        )
                    }
                }

                // ===== ABOUT =====
                item {
                    SettingsSection("ABOUT", Icons.Rounded.Info, GuardianTheme.colors.textSecondary) {
                        InfoRow("Version", "1.0.0 (HoloGlass)")
                        InfoRow("Architecture", "MVVM + Hilt")
                        InfoRow("Local Storage", "Room + DataStore")
                        InfoRow("Security", "EncryptedSharedPreferences")
                    }
                }

                // ===== ACCOUNT =====
                item {
                    SettingsSection("ACCOUNT", Icons.Rounded.Person, GuardianTheme.colors.accentSafe) {
                        Button(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GuardianTheme.colors.accentWarn.copy(alpha = 0.15f),
                                contentColor = GuardianTheme.colors.accentWarn
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SIGN OUT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    // ===== SIGN OUT CONFIRMATION =====
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            shape = RoundedCornerShape(32.dp),
            containerColor = GuardianTheme.colors.glassHigh,
            icon = {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(GuardianTheme.colors.accentWarn.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Logout, null,
                        tint = GuardianTheme.colors.accentWarn, modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text("Sign Out?", fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary)
            },
            text = {
                Text(
                    "You will be redirected to the sign in screen. Your local data will be preserved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GuardianTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut { onSignOut() }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuardianTheme.colors.accentWarn,
                        contentColor = Color.White
                    )
                ) { Text("SIGN OUT", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = GuardianTheme.colors.textMuted)
                }
            }
        )
    }

    if (uiState.showAddContactDialog) {
        AddContactSheet(
            onDismiss = { viewModel.hideAddContactDialog() },
            onConfirm = { name, phone -> viewModel.addContact(name, phone) }
        )
    }
}

// ===== SETTINGS SECTION CARD =====

@Composable
private fun SettingsSection(
    title: String, icon: ImageVector, accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(GuardianTheme.colors.glassHigh, GuardianTheme.colors.glassLow)))
            .border(1.dp, GuardianTheme.colors.glassHigh, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.05f))))
                        .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Text(
                    title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary, letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

// ===== TOGGLE =====

@Composable
private fun HoloGlassToggle(
    title: String, subtitle: String, icon: ImageVector,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit, accent: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "bounce"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) accent.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(GuardianTheme.colors.glassHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GuardianTheme.colors.textMuted)
            }
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = GuardianTheme.colors.textMuted,
                uncheckedTrackColor = GuardianTheme.colors.glassHigh,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ===== CONTACT ROW =====

@Composable
private fun GlassContactRow(contact: EmergencyContact, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GuardianTheme.colors.glassHigh.copy(alpha = 0.6f))
            .border(1.dp, GuardianTheme.colors.glassHigh, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GuardianTheme.colors.accentSafe.copy(alpha = 0.2f), GuardianTheme.colors.accentMain.copy(alpha = 0.1f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name.first().uppercase(),
                    fontWeight = FontWeight.Bold, color = GuardianTheme.colors.accentSafe, fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary)
                Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = GuardianTheme.colors.textMuted)
            }
        }
        IconButton(onClick = onDelete, Modifier.size(36.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GuardianTheme.colors.glassLow), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Remove", tint = GuardianTheme.colors.textMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ===== INFO ROW =====

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = GuardianTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GuardianTheme.colors.accentMain)
    }
}

// ===== ADD CONTACT DIALOG =====

@Composable
private fun AddContactSheet(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = GuardianTheme.colors.glassHigh,
        title = {
            Text("New Contact", fontWeight = FontWeight.Bold, color = GuardianTheme.colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name", color = GuardianTheme.colors.textMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Person, null, tint = GuardianTheme.colors.accentMain) },
                    shape = RoundedCornerShape(16.dp), singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GuardianTheme.colors.accentMain,
                        unfocusedBorderColor = GuardianTheme.colors.glassHigh,
                        focusedContainerColor = GuardianTheme.colors.glassLow.copy(alpha = 0.5f),
                        unfocusedContainerColor = GuardianTheme.colors.glassLow.copy(alpha = 0.3f),
                        cursorColor = GuardianTheme.colors.accentMain,
                        focusedTextColor = GuardianTheme.colors.textPrimary,
                        unfocusedTextColor = GuardianTheme.colors.textPrimary
                    )
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone Number", color = GuardianTheme.colors.textMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = GuardianTheme.colors.accentMain) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp), singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GuardianTheme.colors.accentMain,
                        unfocusedBorderColor = GuardianTheme.colors.glassHigh,
                        focusedContainerColor = GuardianTheme.colors.glassLow.copy(alpha = 0.5f),
                        unfocusedContainerColor = GuardianTheme.colors.glassLow.copy(alpha = 0.3f),
                        cursorColor = GuardianTheme.colors.accentMain,
                        focusedTextColor = GuardianTheme.colors.textPrimary,
                        unfocusedTextColor = GuardianTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onConfirm(name, phone) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GuardianTheme.colors.accentMain, contentColor = Color.White)
            ) { Text("ADD CONTACT", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GuardianTheme.colors.textMuted) }
        }
    )
}

