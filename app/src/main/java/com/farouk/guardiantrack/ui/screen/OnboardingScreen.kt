package com.farouk.guardiantrack.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farouk.guardiantrack.ui.theme.GuardianTheme
import com.farouk.guardiantrack.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Shield,
            title = "Protection 24/7",
            description = "GuardianTrack surveille vos mouvements en temps réel grâce aux capteurs de votre téléphone. Détection automatique des chutes et situations d'urgence.",
            accentColor = GuardianTheme.colors.accentMain
        ),
        OnboardingPage(
            icon = Icons.Filled.Sms,
            title = "Alertes Instantanées",
            description = "En cas d'incident détecté, un SMS d'urgence avec votre position GPS est automatiquement envoyé à vos contacts de confiance.",
            accentColor = GuardianTheme.colors.accentSafe
        ),
        OnboardingPage(
            icon = Icons.Filled.Map,
            title = "Localisation en Direct",
            description = "Visualisez votre position sur la carte en temps réel. Vos données restent sur votre appareil — tout fonctionne même sans internet.",
            accentColor = GuardianTheme.colors.accentWarn
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    // Animated mesh background
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "phase"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ---- Background ----
        val currentAccent = pages[pagerState.currentPage].accentColor
        val meshSecondary = GuardianTheme.colors.meshSecondary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val rad = Math.toRadians(animPhase.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentAccent.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.3f + 0.2f * sin(rad).toFloat()), h * 0.3f),
                    radius = w * 0.7f
                ), radius = w * 0.7f,
                center = Offset(w * (0.3f + 0.2f * sin(rad).toFloat()), h * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshSecondary.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(w * (0.7f + 0.1f * cos(rad).toFloat()), h * 0.7f),
                    radius = w * 0.5f
                ), radius = w * 0.5f,
                center = Offset(w * (0.7f + 0.1f * cos(rad).toFloat()), h * 0.7f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Skip button ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    viewModel.completeOnboarding()
                    onSkip()
                }) {
                    Text(
                        "Passer",
                        color = GuardianTheme.colors.textMuted,
                        fontSize = 14.sp
                    )
                }
            }

            // ---- Pager ----
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // ---- Page indicators ----
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { index ->
                    val isActive = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isActive) 32.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.6f), label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive) pages[index].accentColor
                                else GuardianTheme.colors.textMuted.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Action button ----
            Button(
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding()
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColor,
                    contentColor = GuardianTheme.colors.meshPrimary
                )
            ) {
                Text(
                    if (isLastPage) "Commencer" else "Suivant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (!isLastPage) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ---- Glowing icon orb ----
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = page.accentColor.copy(alpha = 0.12f),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = page.accentColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = page.accentColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GuardianTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            page.description,
            fontSize = 16.sp,
            color = GuardianTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
