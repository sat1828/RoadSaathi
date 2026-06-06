package com.roadsaathi.worker

import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import android.content.Context
import com.roadsaathi.data.local.SyncStatus
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.local.entity.HazardReportEntity
import com.roadsaathi.data.sync.ReportSyncUseCase
import com.roadsaathi.data.sync.SyncResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HazardSyncWorkerTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `doWork returns success when no pending reports`() {
        val dao = mockk<HazardReportDao>()
        val syncUseCase = mockk<ReportSyncUseCase>()

        coEvery { dao.getPendingReportsOnce() } returns emptyList()

        val worker = TestWorker(context, mockk(), dao, syncUseCase)
        val result = runBlocking { worker.doWork() }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork marks report as SYNCED on success`() {
        val dao = mockk<HazardReportDao>()
        val syncUseCase = mockk<ReportSyncUseCase>()

        val entity = HazardReportEntity(
            localId = "test-id",
            hazardType = "POTHOLE",
            latitude = 0.0,
            longitude = 0.0,
            classificationLabel = "pothole",
            confidenceScore = 0.8f,
            reportedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )

        coEvery { dao.getPendingReportsOnce() } returns listOf(entity)
        coEvery { dao.updateSyncStatus(any(), any()) } returns Unit
        coEvery { syncUseCase.sync(any()) } returns SyncResult.Success("remote-123")

        val worker = TestWorker(context, mockk(), dao, syncUseCase)
        val result = runBlocking { worker.doWork() }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify { dao.updateSyncStatus("test-id", SyncStatus.SYNCED) }
    }

    @Test
    fun `doWork marks report as FAILED on client error`() {
        val dao = mockk<HazardReportDao>()
        val syncUseCase = mockk<ReportSyncUseCase>()

        val entity = HazardReportEntity(
            localId = "test-id",
            hazardType = "POTHOLE",
            latitude = 0.0,
            longitude = 0.0,
            classificationLabel = "pothole",
            confidenceScore = 0.8f,
            reportedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )

        coEvery { dao.getPendingReportsOnce() } returns listOf(entity)
        coEvery { dao.updateSyncStatus(any(), any()) } returns Unit
        coEvery { syncUseCase.sync(any()) } returns SyncResult.ClientError(400)

        val worker = TestWorker(context, mockk(), dao, syncUseCase)
        val result = runBlocking { worker.doWork() }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify { dao.updateSyncStatus("test-id", SyncStatus.FAILED) }
    }
}

private class TestWorker(
    context: Context,
    params: WorkerParameters,
    dao: HazardReportDao,
    syncUseCase: ReportSyncUseCase
) : HazardSyncWorker(
    context,
    params,
    dao,
    syncUseCase
)
