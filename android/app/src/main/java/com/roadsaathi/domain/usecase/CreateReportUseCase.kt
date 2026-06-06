package com.roadsaathi.domain.usecase

import android.graphics.Bitmap
import com.roadsaathi.data.repository.HazardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateReportUseCase @Inject constructor(
    private val hazardRepository: HazardRepository
) {
    operator fun invoke(
        bitmap: Bitmap,
        lat: Double,
        lng: Double,
        nhCorridor: String?
    ): Flow<Result<String>> {
        return hazardRepository.saveReport(bitmap, lat, lng, nhCorridor)
    }
}
