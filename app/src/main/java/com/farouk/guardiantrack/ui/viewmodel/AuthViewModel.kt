package com.farouk.guardiantrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farouk.guardiantrack.data.repository.AuthRepository
import com.farouk.guardiantrack.ui.state.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Sign Up and Sign In screens.
 * Handles form validation, auth operations, and UI state.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn()
            if (loggedIn) {
                val name = authRepository.getCurrentUserName()
                _uiState.update { it.copy(isLoggedIn = true, userName = name) }
            }
        }
    }

    // ---- Form field updates ----

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ---- Sign Up ----

    fun signUp(onSuccess: () -> Unit) {
        val state = _uiState.value

        // Validate
        var hasError = false
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Le nom est requis") }
            hasError = true
        }
        if (state.email.isBlank() || !isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Email invalide") }
            hasError = true
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Minimum 6 caractères") }
            hasError = true
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Les mots de passe ne correspondent pas") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.signUp(state.name, state.email, state.password)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userName = state.name.trim(),
                        successMessage = "Compte créé avec succès !"
                    )
                }
                onSuccess()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur lors de l'inscription"
                    )
                }
            }
        }
    }

    // ---- Sign In ----

    fun signIn(onSuccess: () -> Unit) {
        val state = _uiState.value

        var hasError = false
        if (state.email.isBlank() || !isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Email invalide") }
            hasError = true
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Mot de passe requis") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.signIn(state.email, state.password)

            result.onSuccess {
                val name = authRepository.getCurrentUserName()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userName = name,
                        successMessage = "Connexion réussie !"
                    )
                }
                onSuccess()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur de connexion"
                    )
                }
            }
        }
    }

    // ---- Sign Out ----

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update {
                AuthUiState() // Reset to initial state
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
