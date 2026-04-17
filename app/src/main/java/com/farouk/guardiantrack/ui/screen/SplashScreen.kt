package com.farouk.guardiantrack.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farouk.guardiantrack.R
import com.farouk.guardiantrack.ui.theme.*
import com.farouk.guardiantrack.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

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
        delay(2500) // Wait a brief moment for the animation
        when {
            !onboardingCompleted -> onNavigateToOnboarding()
            loggedInUserId > 0  -> onNavigateToDashboard()
            else                -> onNavigateToSignIn()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Gentle pulsing effect for the logo
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoPulse"
    )

    // Entrance animations
    val entranceScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f), label = "entranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(1000), label = "entranceAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(GuardianTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Fluid Background Mesh
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-120).dp)
                .size(400.dp)
                .background(Brush.radialGradient(listOf(GuardianTheme.colors.meshPrimary.copy(alpha = 0.25f), Color.Transparent)), shape = CircleShape)
                .blur(100.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .background(Brush.radialGradient(listOf(GuardianTheme.colors.meshSecondary.copy(alpha = 0.2f), Color.Transparent)), shape = CircleShape)
                .blur(100.dp)
        )

        // Logo Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = entranceAlpha
            }
        ) {
            // Glowing Glass Ring surrounding Logo
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GuardianTheme.colors.glassHigh, GuardianTheme.colors.glassLow)))
                    .border(1.dp, GuardianTheme.colors.accentMain.copy(alpha = 0.5f), CircleShape)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow behind image
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.radialGradient(listOf(GuardianTheme.colors.accentMain.copy(alpha = 0.5f), Color.Transparent)))
                        .blur(20.dp)
                )
                
                // Real App Logo Image
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "GuardianTrack Logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "GUARDIAN",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GuardianTheme.colors.textPrimary,
                letterSpacing = 12.sp,
                modifier = Modifier.graphicsLayer { alpha = entranceAlpha }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GuardianTheme.colors.glassHigh)
                    .border(1.dp, GuardianTheme.colors.glassBorder, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SYSTEM INITIALIZING...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GuardianTheme.colors.accentMain,
                    letterSpacing = 4.sp,
                    modifier = Modifier.graphicsLayer { 
                        alpha = (entranceAlpha * logoScale).coerceIn(0.4f, 1f) 
                    }
                )
            }
        }
    }
}

