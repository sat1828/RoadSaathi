package com.roadsaathi.presentation.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsaathi.domain.usecase.CreateReportUseCase
import com.roadsaathi.ml.ClassificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CameraUiState(
    val isCapturing: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val classificationResult: ClassificationResult? = null,
    val isProcessing: Boolean = false,
    val nhCorridor: String? = null,
    val error: String? = null,
    val savedReportId: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val createReportUseCase: CreateReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onCapture(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCapturing = true,
                    previewBitmap = bitmap,
                    isProcessing = true
                )
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(latitude = lat, longitude = lng) }
    }

    fun updateNhCorridor(corridor: String?) {
        _uiState.update { it.copy(nhCorridor = corridor) }
    }

    fun onConfirm() {
        val bitmap = _uiState.value.previewBitmap ?: return
        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            createReportUseCase(
                bitmap = bitmap,
                lat = lat,
                lng = lng,
                nhCorridor = _uiState.value.nhCorridor
            ).collect { result ->
                result.onSuccess { localId ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            savedReportId = localId,
                            error = null
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = e.message ?: "Failed to save report"
                        )
                    }
                }
            }
        }
    }

    fun onRetake() {
        _uiState.update {
            CameraUiState(
                nhCorridor = it.nhCorridor,
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
