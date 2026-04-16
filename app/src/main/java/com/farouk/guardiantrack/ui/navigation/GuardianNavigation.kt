package com.farouk.guardiantrack.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.farouk.guardiantrack.ui.screen.*

/**
 * Navigation destinations for GuardianTrack.
 *
 * Flow:
 * Splash → [first launch?] → Onboarding → SignUp → Dashboard
 * Splash → [returning, logged in] → Dashboard
 * Splash → [returning, logged out] → SignIn → Dashboard
 * Settings → Sign Out → SignIn
 *
 * Auth is OPTIONAL — "Continuer sans compte" skips to Dashboard.
 */
sealed class Screen(val route: String, val title: String) {
    data object Splash : Screen("splash", "Splash")
    data object Onboarding : Screen("onboarding", "Onboarding")
    data object SignUp : Screen("sign_up", "Inscription")
    data object SignIn : Screen("sign_in", "Connexion")
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object History : Screen("history", "Historique")
    data object Settings : Screen("settings", "Paramètres")
}

/**
 * Navigation host with animated transitions.
 */
@Composable
fun GuardianNavigation(
    navController: NavHostController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { it / 4 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { it / 4 }
        }
    ) {
        // ---- Splash ----
        composable(
            Screen.Splash.route,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(500)) }
        ) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- Onboarding ----
        composable(
            Screen.Onboarding.route,
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(300)) + slideOutHorizontally { -it / 3 } }
        ) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- Sign Up ----
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- Sign In ----
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- Dashboard ----
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }

        // ---- History ----
        composable(Screen.History.route) {
            HistoryScreen()
        }

        // ---- Settings ----
        composable(Screen.Settings.route) {
            SettingsScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onSignOut = {
                    // Clear entire back stack and go to Sign In
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
