package com.roadsaathi.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsaathi.data.repository.HazardRepository
import com.roadsaathi.domain.model.HeatmapCluster
import com.roadsaathi.domain.usecase.GetHeatmapDataUseCase
import com.roadsaathi.domain.usecase.GetPendingReportsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertData(
    val title: String,
    val message: String,
    val hazardType: String,
    val distance: Float
)

data class MapUiState(
    val isLoading: Boolean = false,
    val heatmapClusters: List<HeatmapCluster> = emptyList(),
    val userLocation: Location? = null,
    val pendingReportCount: Int = 0,
    val showIncomingAlert: AlertData? = null,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val hazardRepository: HazardRepository,
    private val getHeatmapDataUseCase: GetHeatmapDataUseCase,
    private val getPendingReportsUseCase: GetPendingReportsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _alertFlow = MutableStateFlow<AlertData?>(null)
    val alertFlow: StateFlow<AlertData?> = _alertFlow.asStateFlow()

    init {
        loadPendingCount()
    }

    fun loadHeatmap(lat: Double, lng: Double, radius: Double = 5000.0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getHeatmapDataUseCase(lat, lng, radius)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { result ->
                    result.onSuccess { clusters ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                heatmapClusters = clusters,
                                error = null
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message)
                        }
                    }
                }
        }
    }

    private fun loadPendingCount() {
        viewModelScope.launch {
            hazardRepository.getPendingCount().collect { count ->
                _uiState.update { it.copy(pendingReportCount = count) }
            }
        }
    }

    fun updateUserLocation(location: Location?) {
        _uiState.update { it.copy(userLocation = location) }
    }

    fun onFabClick() {
        // handled by navigation
    }

    fun onClusterTap(cluster: HeatmapCluster) {
        // Could navigate to detail or show bottom sheet
    }

    fun onAlertDismiss() {
        _uiState.update { it.copy(showIncomingAlert = null) }
    }

    fun showAlert(alert: AlertData) {
        _uiState.update { it.copy(showIncomingAlert = alert) }
    }

    fun autoRefreshHeatmap(intervalMs: Long = 15 * 60 * 1000L) {
        viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                val location = _uiState.value.userLocation
                if (location != null) {
                    loadHeatmap(location.latitude, location.longitude)
                }
            }
        }
    }
}
