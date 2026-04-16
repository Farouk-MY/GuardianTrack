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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Animated mesh
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "phase"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ---- Animated Mesh Gradient Background ----
        val meshTertiary = GuardianTheme.colors.meshTertiary
        val meshPrimary = GuardianTheme.colors.meshPrimary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val rad = Math.toRadians(animPhase.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshTertiary.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w * (0.6f + 0.15f * cos(rad).toFloat()), h * 0.25f),
                    radius = w * 0.55f
                ), radius = w * 0.55f,
                center = Offset(w * (0.6f + 0.15f * cos(rad).toFloat()), h * 0.25f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshPrimary.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * (0.3f + 0.1f * sin(rad).toFloat()), h * 0.65f),
                    radius = w * 0.5f
                ), radius = w * 0.5f,
                center = Offset(w * (0.3f + 0.1f * sin(rad).toFloat()), h * 0.65f)
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
            Spacer(Modifier.height(80.dp))

            // ---- Header ----
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = GuardianTheme.colors.accentMain
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Bon retour !",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GuardianTheme.colors.textPrimary
            )
            Text(
                "Connectez-vous pour retrouver vos données",
                fontSize = 14.sp,
                color = GuardianTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(40.dp))

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

            // ---- Sign In Button ----
            Button(
                onClick = { viewModel.signIn(onSignInSuccess) },
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
                        "Se connecter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Navigate to Sign Up ----
            TextButton(onClick = onNavigateToSignUp) {
                Text(
                    "Pas encore de compte ? ",
                    color = GuardianTheme.colors.textMuted,
                    fontSize = 14.sp
                )
                Text(
                    "Créer un compte",
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
