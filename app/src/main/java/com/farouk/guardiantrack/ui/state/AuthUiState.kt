package com.farouk.guardiantrack.ui.state

/**
 * Immutable UI state for authentication screens (SignUp + SignIn).
 */
data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)
