package com.roadsaathi.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.roadsaathi.data.local.SyncStatus
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.local.toLocalReport
import com.roadsaathi.data.sync.ReportSyncUseCase
import com.roadsaathi.data.sync.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class HazardSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val hazardReportDao: HazardReportDao,
    private val reportSyncUseCase: ReportSyncUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingReports = hazardReportDao.getPendingReportsOnce()
        val total = pendingReports.size
        if (total == 0) return Result.success()

        var hasRetryableFailure = false

        for ((index, report) in pendingReports.withIndex()) {
            val current = index + 1
            setProgress(
                Data.Builder()
                    .putInt("progress", current)
                    .putInt("total", total)
                    .putString("localId", report.localId)
                    .build()
            )

            try {
                hazardReportDao.updateSyncStatus(report.localId, SyncStatus.IN_FLIGHT)

                when (val result = reportSyncUseCase.sync(report.toLocalReport())) {
                    is SyncResult.Success -> {
                        hazardReportDao.updateSyncStatus(report.localId, SyncStatus.SYNCED)
                        if (result.remoteId != null) {
                            hazardReportDao.updateRemoteId(report.localId, result.remoteId)
                        }
                    }
                    is SyncResult.ClientError -> {
                        hazardReportDao.updateSyncStatus(report.localId, SyncStatus.FAILED)
                    }
                    is SyncResult.Error -> {
                        hazardReportDao.updateSyncStatus(report.localId, SyncStatus.PENDING)
                        hasRetryableFailure = true
                    }
                }
            } catch (e: Exception) {
                hazardReportDao.updateSyncStatus(report.localId, SyncStatus.PENDING)
                hasRetryableFailure = true
            }
        }

        return if (hasRetryableFailure) Result.retry() else Result.success()
    }
}
