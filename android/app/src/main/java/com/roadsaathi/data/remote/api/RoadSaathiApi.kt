package com.roadsaathi.data.remote.api

import com.roadsaathi.data.remote.dto.AuthResponse
import com.roadsaathi.data.remote.dto.DriverSessionRequest
import com.roadsaathi.data.remote.dto.HazardReportResponse
import com.roadsaathi.data.remote.dto.HeatmapResponse
import com.roadsaathi.data.remote.dto.LoginRequest
import com.roadsaathi.data.remote.dto.RegisterRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface RoadSaathiApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @Multipart
    @POST("reports")
    suspend fun createReport(
        @Part photo: MultipartBody.Part?,
        @Part("report") report: RequestBody
    ): Response<HazardReportResponse>

    @GET("reports")
    suspend fun getReports(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double
    ): Response<ResponseBody>

    @POST("reports/{id}/confirm")
    suspend fun confirmReport(@Path("id") id: String): Response<ResponseBody>

    @POST("reports/{id}/dismiss")
    suspend fun dismissReport(@Path("id") id: String): Response<ResponseBody>

    @GET("reports/s3-upload-url")
    suspend fun getS3UploadUrl(
        @Query("fileName") fileName: String,
        @Query("contentType") contentType: String
    ): Response<ResponseBody>

    @POST("drivers/session")
    suspend fun upsertSession(@Body request: DriverSessionRequest): Response<ResponseBody>

    @GET("hazards/heatmap")
    suspend fun getHeatmap(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double
    ): Response<HeatmapResponse>

    @GET("admin/triage")
    suspend fun getAdminTriage(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("admin/reports/{id}/assign")
    suspend fun assignReport(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("admin/reports/{id}/status")
    suspend fun updateReportStatus(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}
