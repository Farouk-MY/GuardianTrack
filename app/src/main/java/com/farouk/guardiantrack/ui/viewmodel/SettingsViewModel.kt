package com.farouk.guardiantrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farouk.guardiantrack.data.repository.AuthRepository
import com.farouk.guardiantrack.data.repository.ContactRepository
import com.farouk.guardiantrack.data.repository.PreferencesRepository
import com.farouk.guardiantrack.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages user preferences, emergency contacts, and app configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val contactRepository: ContactRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        loadContacts()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.sensitivityThreshold.collect { value ->
                _uiState.update { it.copy(sensitivityThreshold = value) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.darkMode.collect { value ->
                _uiState.update { it.copy(isDarkMode = value) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.smsSimulationMode.collect { value ->
                _uiState.update { it.copy(isSmsSimulation = value) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.serviceEnabled.collect { value ->
                _uiState.update { it.copy(isServiceEnabled = value) }
            }
        }
        // Load encrypted preferences
        _uiState.update { it.copy(emergencyNumber = preferencesRepository.emergencyNumber) }
    }

    private fun loadContacts() {
        viewModelScope.launch {
            contactRepository.getAllContacts().collect { contacts ->
                _uiState.update { it.copy(emergencyContacts = contacts) }
            }
        }
    }

    fun setSensitivityThreshold(value: Float) {
        viewModelScope.launch {
            preferencesRepository.setSensitivityThreshold(value)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun setSmsSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmsSimulationMode(enabled)
        }
    }

    fun setEmergencyNumber(number: String) {
        preferencesRepository.emergencyNumber = number
        _uiState.update { it.copy(emergencyNumber = number) }
    }

    fun showAddContactDialog() {
        _uiState.update { it.copy(showAddContactDialog = true) }
    }

    fun hideAddContactDialog() {
        _uiState.update { it.copy(showAddContactDialog = false) }
    }

    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            contactRepository.addContact(name, phoneNumber)
            _uiState.update {
                it.copy(
                    showAddContactDialog = false,
                    message = "Contact ajouté: $name"
                )
            }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
