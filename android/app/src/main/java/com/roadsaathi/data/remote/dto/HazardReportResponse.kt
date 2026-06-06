package com.roadsaathi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HazardReportResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("hazard_type")
    val hazardType: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("photo_url")
    val photoUrl: String? = null,
    @SerializedName("classification_label")
    val classificationLabel: String,
    @SerializedName("confidence_score")
    val confidenceScore: Float,
    @SerializedName("reported_at")
    val reportedAt: Long,
    @SerializedName("nh_corridor")
    val nhCorridor: String? = null,
    @SerializedName("severity")
    val severity: Int = 1,
    @SerializedName("status")
    val status: String,
    @SerializedName("expires_at")
    val expiresAt: Long,
    @SerializedName("confirm_count")
    val confirmCount: Int = 0,
    @SerializedName("ai_brief")
    val aiBrief: String? = null,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long
)
