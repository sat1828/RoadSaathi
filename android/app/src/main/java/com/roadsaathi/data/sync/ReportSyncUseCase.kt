package com.roadsaathi.data.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.roadsaathi.data.local.LocalHazardReport
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.remote.ApiClient
import com.roadsaathi.data.remote.dto.HazardReportRequest
import com.roadsaathi.data.repository.ImageRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportSyncUseCase @Inject constructor(
    private val hazardReportDao: HazardReportDao,
    private val apiClient: ApiClient,
    private val imageRepository: ImageRepository
) {
    private val gson = Gson()

    suspend fun sync(report: LocalHazardReport): SyncResult {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val reportedAtIso = isoFormat.format(Date(report.reportedAt))

        val contentType = "image/jpeg"
        val photoFile = report.photoLocalPath?.let { imageRepository.getFile(it) }
        val s3Response = apiClient.api.getS3UploadUrl(
            fileName = report.localId + ".jpg",
            contentType = contentType
        )
        if (!s3Response.isSuccessful) {
            return SyncResult.Error("Failed to get S3 upload URL: ${s3Response.code()}")
        }
        val bodyString = s3Response.body()?.string() ?: return SyncResult.Error("Empty S3 URL")
        val s3Json = try {
            gson.fromJson(bodyString, JsonObject::class.java)
        } catch (e: Exception) {
            return SyncResult.Error("Failed to parse S3 response")
        }
        val presignedUrl = s3Json.get("url")?.asString ?: return SyncResult.Error("Missing 'url' in S3 response")

        if (photoFile != null && photoFile.exists()) {
            val fileBody = photoFile.asRequestBody(contentType.toMediaType())
            val uploadReq = Request.Builder()
                .url(presignedUrl)
                .put(fileBody)
                .build()
            val uploadClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()
            val uploadResp = uploadClient.newCall(uploadReq).execute()
            if (!uploadResp.isSuccessful) {
                return SyncResult.Error("S3 upload failed: ${uploadResp.code()}")
            }
        }

        val photoUrl = presignedUrl.substringBefore("?")
        val reportRequest = HazardReportRequest(
            hazardType = report.hazardType,
            latitude = report.latitude,
            longitude = report.longitude,
            photoUrl = photoUrl,
            classificationLabel = report.classificationLabel,
            confidenceScore = report.confidenceScore,
            reportedAt = reportedAtIso,
            nhCorridor = report.nhCorridor,
            severity = report.severity
        )
        val reportJson = gson.toJson(reportRequest)
        val reportPart = reportJson.toRequestBody("application/json".toMediaType())
        val createResp = apiClient.api.createReport(photo = null, report = reportPart)

        return if (createResp.isSuccessful) {
            val body = createResp.body()
            SyncResult.Success(body?.id)
        } else {
            val code = createResp.code()
            if (code in 400..499) {
                SyncResult.ClientError(code)
            } else {
                SyncResult.Error("Server error: $code")
            }
        }
    }
}

sealed class SyncResult {
    data class Success(val remoteId: String?) : SyncResult()
    data class ClientError(val code: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
