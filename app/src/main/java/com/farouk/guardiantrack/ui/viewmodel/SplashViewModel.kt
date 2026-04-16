package com.farouk.guardiantrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Splash screen.
 * Provides onboarding and login state so the splash can route correctly:
 *
 * - First launch (onboarding not done) → Onboarding
 * - Returning user, logged in → Dashboard
 * - Returning user, logged out → Sign In
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    preferencesManager: UserPreferencesManager
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean> = preferencesManager.onboardingCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val loggedInUserId: StateFlow<Long> = preferencesManager.loggedInUserId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = -1L
        )
}
