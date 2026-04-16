package com.farouk.guardiantrack.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farouk.guardiantrack.ui.theme.GuardianTheme
import com.farouk.guardiantrack.ui.viewmodel.AuthViewModel
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Animated mesh background
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "phase"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ---- Animated Mesh Gradient Background ----
        val meshSecondary = GuardianTheme.colors.meshSecondary
        val meshTertiary = GuardianTheme.colors.meshTertiary
        val meshPrimary = GuardianTheme.colors.meshPrimary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val rad = Math.toRadians(animPhase.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshSecondary.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w * (0.3f + 0.15f * sin(rad).toFloat()), h * 0.2f),
                    radius = w * 0.6f
                ), radius = w * 0.6f,
                center = Offset(w * (0.3f + 0.15f * sin(rad).toFloat()), h * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshTertiary.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * (0.7f + 0.1f * cos(rad).toFloat()), h * 0.7f),
                    radius = w * 0.5f
                ), radius = w * 0.5f,
                center = Offset(w * (0.7f + 0.1f * cos(rad).toFloat()), h * 0.7f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshPrimary.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(w * 0.5f, h * (0.4f + 0.1f * sin(rad * 1.3).toFloat())),
                    radius = w * 0.4f
                ), radius = w * 0.4f,
                center = Offset(w * 0.5f, h * (0.4f + 0.1f * sin(rad * 1.3).toFloat()))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ---- Header ----
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = GuardianTheme.colors.accentMain
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Créer un compte",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GuardianTheme.colors.textPrimary
            )
            Text(
                "Protégez vos données et accédez partout",
                fontSize = 14.sp,
                color = GuardianTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(32.dp))

            // ---- Glass Card Form ----
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = GuardianTheme.colors.glassHigh,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            GuardianTheme.colors.glassBorder,
                            GuardianTheme.colors.glassBorder.copy(alpha = 0.3f)
                        )
                    )
                ),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name
                    HoloGlassTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = "Nom complet",
                        icon = Icons.Outlined.Person,
                        error = uiState.nameError,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // Email
                    HoloGlassTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = "Adresse email",
                        icon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                        error = uiState.emailError,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // Password
                    HoloGlassPasswordField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = "Mot de passe",
                        error = uiState.passwordError,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // Confirm Password
                    HoloGlassPasswordField(
                        value = uiState.confirmPassword,
                        onValueChange = { viewModel.updateConfirmPassword(it) },
                        label = "Confirmer le mot de passe",
                        error = uiState.confirmPasswordError,
                        imeAction = ImeAction.Done,
                        onImeAction = { focusManager.clearFocus() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Error message ----
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = GuardianTheme.colors.accentDanger.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline, null,
                            tint = GuardianTheme.colors.accentDanger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.errorMessage ?: "",
                            color = GuardianTheme.colors.accentDanger,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Sign Up Button ----
            Button(
                onClick = { viewModel.signUp(onSignUpSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardianTheme.colors.accentMain,
                    contentColor = GuardianTheme.colors.meshPrimary
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GuardianTheme.colors.meshPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Créer mon compte",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Navigate to Sign In ----
            TextButton(onClick = onNavigateToSignIn) {
                Text(
                    "Déjà un compte ? ",
                    color = GuardianTheme.colors.textMuted,
                    fontSize = 14.sp
                )
                Text(
                    "Se connecter",
                    color = GuardianTheme.colors.accentMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // ---- Skip button ----
            TextButton(onClick = onSkip) {
                Text(
                    "Continuer sans compte",
                    color = GuardianTheme.colors.textMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// =============================================
// Shared HoloGlass Form Components
// =============================================

@Composable
fun HoloGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = GuardianTheme.colors.textMuted)
            },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GuardianTheme.colors.accentMain,
                unfocusedBorderColor = GuardianTheme.colors.glassBorder,
                errorBorderColor = GuardianTheme.colors.accentDanger,
                focusedLabelColor = GuardianTheme.colors.accentMain,
                unfocusedLabelColor = GuardianTheme.colors.textMuted,
                cursorColor = GuardianTheme.colors.accentMain,
                focusedTextColor = GuardianTheme.colors.textPrimary,
                unfocusedTextColor = GuardianTheme.colors.textPrimary,
                focusedContainerColor = GuardianTheme.colors.glassLow,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            isError = error != null
        )
        if (error != null) {
            Text(
                error,
                color = GuardianTheme.colors.accentDanger,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun HoloGlassPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = GuardianTheme.colors.textMuted)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Masquer" else "Afficher",
                        tint = GuardianTheme.colors.textMuted
                    )
                }
            },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GuardianTheme.colors.accentMain,
                unfocusedBorderColor = GuardianTheme.colors.glassBorder,
                errorBorderColor = GuardianTheme.colors.accentDanger,
                focusedLabelColor = GuardianTheme.colors.accentMain,
                unfocusedLabelColor = GuardianTheme.colors.textMuted,
                cursorColor = GuardianTheme.colors.accentMain,
                focusedTextColor = GuardianTheme.colors.textPrimary,
                unfocusedTextColor = GuardianTheme.colors.textPrimary,
                focusedContainerColor = GuardianTheme.colors.glassLow,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            isError = error != null
        )
        if (error != null) {
            Text(
                error,
                color = GuardianTheme.colors.accentDanger,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
