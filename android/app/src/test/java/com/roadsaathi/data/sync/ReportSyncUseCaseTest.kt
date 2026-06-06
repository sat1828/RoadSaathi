package com.roadsaathi.data.sync

import com.roadsaathi.data.local.LocalHazardReport
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.remote.ApiClient
import com.roadsaathi.data.remote.api.RoadSaathiApi
import com.roadsaathi.data.repository.ImageRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

class ReportSyncUseCaseTest {

    private val dao = mockk<HazardReportDao>()
    private val apiClient = mockk<ApiClient>()
    private val imageRepository = mockk<ImageRepository>()
    private val api = mockk<RoadSaathiApi>()

    private val useCase = ReportSyncUseCase(dao, apiClient, imageRepository)

    private val testReport = LocalHazardReport(
        localId = "test-id",
        hazardType = "POTHOLE",
        latitude = 28.6139,
        longitude = 77.2090,
        photoLocalPath = null,
        classificationLabel = "pothole",
        confidenceScore = 0.85f,
        reportedAt = 1000L,
        nhCorridor = "NH-44",
        severity = 2
    )

    @Test
    fun `sync returns Error when getS3UploadUrl fails`() = runBlocking {
        coEvery { apiClient.api } returns api
        coEvery { api.getS3UploadUrl(any(), any()) } returns Response.error(500, "".toResponseBody(null))

        val result = useCase.sync(testReport)
        assertThat(result).isInstanceOf(SyncResult.Error::class.java)
    }
}
