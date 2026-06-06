package com.roadsaathi.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsaathi.data.local.entity.HazardReportEntity
import com.roadsaathi.data.repository.HazardRepository
import com.roadsaathi.data.repository.SyncProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val reports: List<HazardReportEntity> = emptyList(),
    val isLoading: Boolean = false,
    val syncProgress: SyncProgress? = null,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val hazardRepository: HazardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            hazardRepository.getAllReports()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { reports ->
                    _uiState.update {
                        it.copy(reports = reports, isLoading = false, error = null)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, syncProgress = null) }
            hazardRepository.syncAllPending()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { progress ->
                    _uiState.update { it.copy(syncProgress = progress) }
                }
            _uiState.update { it.copy(isLoading = false, syncProgress = null) }
        }
    }

    fun deleteReport(localId: String) {
        viewModelScope.launch {
            hazardRepository.deleteReport(localId)
        }
    }
}
