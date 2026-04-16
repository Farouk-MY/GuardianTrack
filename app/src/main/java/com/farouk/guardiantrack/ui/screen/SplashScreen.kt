package com.farouk.guardiantrack.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farouk.guardiantrack.ui.theme.*
import com.farouk.guardiantrack.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * HoloGlass Splash Screen
 * Full-screen fluid cinematic boot sequence.
 *
 * Navigation routing:
 * 1. First launch (onboarding not completed) → Onboarding
 * 2. Returning user, logged in → Dashboard
 * 3. Returning user, logged out → Sign In
 */
@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val loggedInUserId by viewModel.loggedInUserId.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        delay(3200)
        when {
            !onboardingCompleted -> onNavigateToOnboarding()
            loggedInUserId > 0  -> onNavigateToDashboard()
            else                -> onNavigateToSignIn()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Morphing orb physics
    val orbMorphX by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ox"
    )
    val orbMorphY by infiniteTransition.animateFloat(
        initialValue = 1.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "oy"
    )
    
    // Background mesh rotation
    val bgRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "bgr"
    )

    // Entrance animations
    val orbScale by animateFloatAsState(
        targetValue = if (started) 1f else 0f, 
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "osc"
    )
    
    val titleAppears by animateFloatAsState(
        targetValue = if (started) 1f else 0f, 
        animationSpec = tween(1200, 800, LinearOutSlowInEasing), label = "titleA"
    )
    
    val subtitleAppears by animateFloatAsState(
        targetValue = if (started) 1f else 0f, 
        animationSpec = tween(1200, 1400, LinearOutSlowInEasing), label = "subA"
    )

    val bgPrimary = GuardianTheme.colors.meshPrimary
    val bgSecondary = GuardianTheme.colors.meshSecondary
    val bgTertiary = GuardianTheme.colors.meshTertiary
    
    val accent = GuardianTheme.colors.accentMain
    val textPrimary = GuardianTheme.colors.textPrimary
    val glassHigh = GuardianTheme.colors.glassHigh
    val glassBorder = GuardianTheme.colors.glassBorder

    Box(
        modifier = Modifier.fillMaxSize().background(bgPrimary),
        contentAlignment = Alignment.Center
    ) {
        // ---- Fluid Background Mesh & Morphing Orb ----
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height * 0.40f
            
            // Rotating Mesh Background Layers
            val bgRadius = size.maxDimension * 0.8f
            
            // Mesh 1
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(bgSecondary.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(cx + cos(Math.toRadians(bgRotation.toDouble())).toFloat() * 300f, 
                                    cy + sin(Math.toRadians(bgRotation.toDouble())).toFloat() * 300f),
                    radius = bgRadius
                )
            )
            
            // Mesh 2
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(bgTertiary.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(cx + cos(Math.toRadians((bgRotation+180).toDouble())).toFloat() * -200f, 
                                    cy + sin(Math.toRadians((bgRotation+180).toDouble())).toFloat() * 400f),
                    radius = bgRadius * 1.2f
                )
            )

            // Dynamic Morphing App Orb
            val baseOrbRadius = 80.dp.toPx() * orbScale
            
            // Outer Halo
            if (baseOrbRadius > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = baseOrbRadius * 2.5f
                    )
                )
                
                // Replace path morphing with layered soft glowing circles representing liquid
                drawCircle(
                    color = glassHigh,
                    radius = baseOrbRadius * orbMorphX,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = accent.copy(alpha = 0.6f),
                    radius = baseOrbRadius * orbMorphY * 0.85f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = glassBorder,
                    radius = baseOrbRadius * orbMorphX,
                    style = Stroke(width = 8f),
                    center = Offset(cx, cy)
                )
            }
        }

        // ---- Text Layer ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "G U A R D I A N",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary.copy(alpha = titleAppears),
                letterSpacing = 10.sp
            )
            Text(
                text = "H O L O G L A S S    E D I T I O N",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent.copy(alpha = subtitleAppears),
                letterSpacing = 6.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
