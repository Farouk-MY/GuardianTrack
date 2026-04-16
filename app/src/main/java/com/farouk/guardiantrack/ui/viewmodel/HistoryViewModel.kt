package com.farouk.guardiantrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farouk.guardiantrack.data.local.preferences.UserPreferencesManager
import com.farouk.guardiantrack.data.repository.IncidentRepository
import com.farouk.guardiantrack.domain.model.IncidentType
import com.farouk.guardiantrack.ui.state.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 * Survives configuration changes.
 * Provides paginated incident list, deletion, and CSV export.
 * All queries are scoped to the current logged-in user.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val userPrefs: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userPrefs.getLoggedInUserId()
            incidentRepository.getAllIncidents(userId).collect { incidents ->
                val filtered = _uiState.value.filterType?.let { filter ->
                    incidents.filter { it.type.name == filter }
                } ?: incidents

                _uiState.update {
                    it.copy(incidents = filtered, isLoading = false)
                }
            }
        }
    }

    fun deleteIncident(id: Long) {
        viewModelScope.launch {
            incidentRepository.deleteIncident(id)
        }
    }

    fun deleteAllIncidents() {
        viewModelScope.launch {
            incidentRepository.deleteAllIncidents()
        }
    }

    fun exportToCSV() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fileName = incidentRepository.exportIncidentsToCSV()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        exportResult = "Exporté: $fileName"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur d'export: ${e.message}"
                    )
                }
            }
        }
    }

    fun setFilter(type: String?) {
        _uiState.update { it.copy(filterType = type) }
        loadIncidents()
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
