package com.roadsaathi.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsaathi.data.repository.HazardRepository
import com.roadsaathi.data.repository.SyncProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncProgress: SyncProgress? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val hazardRepository: HazardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hazardRepository.getPendingCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingCount = count)
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            hazardRepository.syncAllPending().collect { progress ->
                _uiState.value = _uiState.value.copy(syncProgress = progress)
            }
            _uiState.value = _uiState.value.copy(isSyncing = false, syncProgress = null)
        }
    }

    fun logout() {
        // Clear local data and session
        viewModelScope.launch {
            hazardRepository.deleteAll()
        }
    }
}
