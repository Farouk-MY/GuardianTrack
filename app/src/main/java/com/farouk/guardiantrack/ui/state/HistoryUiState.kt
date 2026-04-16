package com.farouk.guardiantrack.ui.state

import com.farouk.guardiantrack.domain.model.Incident

/**
 * Immutable UI state for the History screen.
 */
data class HistoryUiState(
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = false,
    val exportResult: String? = null,
    val errorMessage: String? = null,
    val filterType: String? = null // null = all types
)
