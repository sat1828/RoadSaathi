package com.roadsaathi.domain.usecase

import com.roadsaathi.data.repository.HazardRepository
import com.roadsaathi.data.repository.SyncProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncReportsUseCase @Inject constructor(
    private val hazardRepository: HazardRepository
) {
    operator fun invoke(): Flow<SyncProgress> {
        return hazardRepository.syncAllPending()
    }
}
