package com.farouk.guardiantrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * HoloGlass Design System Token Architecture
 * A beautifully vibrant, fully fluid aesthetic mixing glassmorphic overlays with
 * mesh gradient backgrounds.
 */
data class HoloGlassColors(
    val background: Color,
    
    val meshPrimary: Color,
    val meshSecondary: Color,
    val meshTertiary: Color,
    
    val glassLow: Color,     // Very subtle translucent layer
    val glassHigh: Color,    // More opaque elevated layer
    val glassBorder: Color,  // Specular edge highlight
    
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    
    val accentMain: Color,   // Primary interaction
    val accentSafe: Color,   // Success/Active
    val accentDanger: Color, // Alerts/SOS
    val accentWarn: Color    // Warnings
)

val DarkHoloGlass = HoloGlassColors(
    background = Color(0xFF070012),    // Near-black cosmic
    
    meshPrimary = Color(0xFF140033),   // Deep cosmic violet
    meshSecondary = Color(0xFF6B0047), // Dark magenta
    meshTertiary = Color(0xFF003D66),  // Deep ocean cyan
    
    glassLow = Color(0xFFFFFFFF).copy(alpha = 0.04f),
    glassHigh = Color(0xFFFFFFFF).copy(alpha = 0.08f),
    glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f),
    
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB4B9C8), // Soft iridescent blue-grey
    textMuted = Color(0xFF707A94),
    textDisabled = Color(0xFF404860),
    
    accentMain = Color(0xFF00FFFF), // Electric Cyan
    accentSafe = Color(0xFF00FF88), // Neon Green
    accentDanger = Color(0xFFFF0055), // Hot Pink/Red
    accentWarn = Color(0xFFFFB700)
)

val LightHoloGlass = HoloGlassColors(
    background = Color(0xFFF0F2F8),     // Soft pearl white
    
    meshPrimary = Color(0xFFD3E4FF),     // Pastel Powder Blue
    meshSecondary = Color(0xFFFFD1EC),   // Soft Peach/Magenta
    meshTertiary = Color(0xFFC0FFF4),    // Bright mint cyan
    
    glassLow = Color(0xFFFFFFFF).copy(alpha = 0.4f),
    glassHigh = Color(0xFFFFFFFF).copy(alpha = 0.7f),
    glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.9f),
    
    textPrimary = Color(0xFF0A1430),
    textSecondary = Color(0xFF4C587A),
    textMuted = Color(0xFF818CAB),
    textDisabled = Color(0xFFB0B8CC),
    
    accentMain = Color(0xFF0066FF), // Deep vibrant blue
    accentSafe = Color(0xFF00A355), // Bold green
    accentDanger = Color(0xFFE6004C), // Sharp crimson
    accentWarn = Color(0xFFCC7A00)
)

val LocalHoloGlassColors = staticCompositionLocalOf { DarkHoloGlass }

object GuardianTheme {
    val colors: HoloGlassColors
        @Composable
        get() = LocalHoloGlassColors.current
}

// Material 3 mapping for standardized components if needed
private val MaterialDark = darkColorScheme(background = Color.Black)
private val MaterialLight = lightColorScheme(background = Color.White)

@Composable
fun GuardianTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val customColors = if (darkTheme) DarkHoloGlass else LightHoloGlass
    val colorScheme = if (darkTheme) MaterialDark else MaterialLight

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use purely transparent system bars to let HoloGlass flow underneath perfectly
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalHoloGlassColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}