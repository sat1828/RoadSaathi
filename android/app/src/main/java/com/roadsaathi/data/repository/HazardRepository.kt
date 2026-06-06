package com.roadsaathi.data.repository

import android.graphics.Bitmap
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.roadsaathi.data.local.SyncStatus
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.local.entity.HazardReportEntity
import com.roadsaathi.data.local.toLocalReport
import com.roadsaathi.data.remote.ApiClient
import com.roadsaathi.data.remote.dto.HeatmapResponse
import com.roadsaathi.data.sync.ReportSyncUseCase
import com.roadsaathi.data.sync.SyncResult
import com.roadsaathi.ml.TFLiteClassifier
import com.roadsaathi.worker.HazardSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SyncProgress(
    val current: Int,
    val total: Int,
    val localId: String,
    val status: SyncStatus
)

@Singleton
class HazardRepository @Inject constructor(
    private val hazardReportDao: HazardReportDao,
    private val apiClient: ApiClient,
    private val tfliteClassifier: TFLiteClassifier,
    private val imageRepository: ImageRepository,
    private val workManager: WorkManager,
    private val reportSyncUseCase: ReportSyncUseCase
) {
    fun saveReport(
        bitmap: Bitmap,
        lat: Double,
        lng: Double,
        nhCorridor: String?
    ): Flow<Result<String>> = flow {
        try {
            val classification = withContext(Dispatchers.Default) {
                tfliteClassifier.classify(bitmap)
            }
            val localId = UUID.randomUUID().toString()
            val photoPath = imageRepository.saveBitmapWithExif(bitmap, lat, lng)
            val now = System.currentTimeMillis()

            val entity = HazardReportEntity(
                localId = localId,
                hazardType = classification.label.replace(" ", "_").uppercase(),
                latitude = lat,
                longitude = lng,
                photoLocalPath = photoPath,
                classificationLabel = classification.label,
                confidenceScore = classification.confidence,
                reportedAt = now,
                syncStatus = SyncStatus.PENDING,
                remoteId = null,
                nhCorridor = nhCorridor,
                severity = 1
            )

            hazardReportDao.insertReport(entity)
            enqueueSync()
            emit(Result.success(localId))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncWork = OneTimeWorkRequestBuilder<HazardSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .build()
        workManager.enqueueUniqueWork(
            "hazard_sync",
            ExistingWorkPolicy.APPEND,
            syncWork
        )
    }

    fun getPendingReports(): Flow<List<HazardReportEntity>> = hazardReportDao.getPendingReports()
    fun getAllReports(): Flow<List<HazardReportEntity>> = hazardReportDao.getAllReports()
    fun getPendingCount(): Flow<Int> = hazardReportDao.getPendingCount()

    fun syncReport(localId: String): Flow<Result<Unit>> = flow {
        try {
            hazardReportDao.updateSyncStatus(localId, SyncStatus.IN_FLIGHT)
            val entity = hazardReportDao.getReportById(localId).first()
                ?: throw Exception("Report not found")

            when (val result = reportSyncUseCase.sync(entity.toLocalReport())) {
                is SyncResult.Success -> {
                    hazardReportDao.updateSyncStatus(localId, SyncStatus.SYNCED)
                    if (result.remoteId != null) {
                        hazardReportDao.updateRemoteId(localId, result.remoteId)
                    }
                    emit(Result.success(Unit))
                }
                is SyncResult.ClientError -> {
                    hazardReportDao.updateSyncStatus(localId, SyncStatus.FAILED)
                    emit(Result.failure(Exception("Client error: ${result.code}")))
                }
                is SyncResult.Error -> {
                    hazardReportDao.updateSyncStatus(localId, SyncStatus.PENDING)
                    emit(Result.failure(Exception(result.message)))
                }
            }
        } catch (e: Exception) {
            hazardReportDao.updateSyncStatus(localId, SyncStatus.PENDING)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun syncAllPending(): Flow<SyncProgress> = flow {
        val reports = hazardReportDao.getPendingReportsOnce()
        val total = reports.size
        var current = 0

        for (report in reports) {
            current++
            try {
                hazardReportDao.updateSyncStatus(report.localId, SyncStatus.IN_FLIGHT)
                emit(SyncProgress(current, total, report.localId, SyncStatus.IN_FLIGHT))

                when (val result = reportSyncUseCase.sync(report.toLocalReport())) {
                    is SyncResult.Success -> {
                        hazardReportDao.updateSyncStatus(report.localId, SyncStatus.SYNCED)
                        if (result.remoteId != null) {
                            hazardReportDao.updateRemoteId(report.localId, result.remoteId)
                        }
                        emit(SyncProgress(current, total, report.localId, SyncStatus.SYNCED))
                    }
                    is SyncResult.ClientError -> {
                        hazardReportDao.updateSyncStatus(report.localId, SyncStatus.FAILED)
                        emit(SyncProgress(current, total, report.localId, SyncStatus.FAILED))
                    }
                    is SyncResult.Error -> {
                        hazardReportDao.updateSyncStatus(report.localId, SyncStatus.PENDING)
                        emit(SyncProgress(current, total, report.localId, SyncStatus.PENDING))
                    }
                }
            } catch (e: Exception) {
                hazardReportDao.updateSyncStatus(report.localId, SyncStatus.PENDING)
                emit(SyncProgress(current, total, report.localId, SyncStatus.PENDING))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun fetchHeatmapData(lat: Double, lng: Double, radius: Double): Flow<Result<HeatmapResponse>> = flow {
        try {
            val response = apiClient.api.getHeatmap(lat, lng, radius)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    emit(Result.success(body))
                } else {
                    emit(Result.failure(Exception("Empty heatmap response")))
                }
            } else {
                emit(Result.failure(Exception("Failed to fetch heatmap: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun confirmReport(remoteId: String): Flow<Result<Unit>> = flow {
        try {
            val response = apiClient.api.confirmReport(remoteId)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to confirm: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun dismissReport(remoteId: String): Flow<Result<Unit>> = flow {
        try {
            val response = apiClient.api.dismissReport(remoteId)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to dismiss: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getReportById(localId: String): HazardReportEntity? {
        return hazardReportDao.getReportById(localId).first()
    }

    suspend fun deleteReport(localId: String) {
        hazardReportDao.deleteReport(localId)
    }

    suspend fun deleteAll() {
        hazardReportDao.deleteAll()
    }
}
