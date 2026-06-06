package com.roadsaathi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HazardReportRequest(
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
    val reportedAt: String,
    @SerializedName("nh_corridor")
    val nhCorridor: String? = null,
    @SerializedName("severity")
    val severity: Int = 1
)
