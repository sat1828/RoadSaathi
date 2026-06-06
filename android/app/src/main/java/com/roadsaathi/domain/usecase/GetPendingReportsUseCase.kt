package com.roadsaathi.domain.usecase

import com.roadsaathi.data.repository.HazardRepository
import com.roadsaathi.domain.model.HazardReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPendingReportsUseCase @Inject constructor(
    private val hazardRepository: HazardRepository
) {
    operator fun invoke(): Flow<List<HazardReport>> {
        return hazardRepository.getPendingReports().map { entities ->
            entities.map { HazardReport.fromEntity(it) }
        }
    }
}
