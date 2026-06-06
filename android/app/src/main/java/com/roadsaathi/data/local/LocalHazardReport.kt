package com.roadsaathi.data.local

import com.roadsaathi.data.local.entity.HazardReportEntity

data class LocalHazardReport(
    val localId: String,
    val hazardType: String,
    val latitude: Double,
    val longitude: Double,
    val photoLocalPath: String?,
    val classificationLabel: String?,
    val confidenceScore: Float?,
    val reportedAt: Long,
    val nhCorridor: String?,
    val severity: Int
)

fun HazardReportEntity.toLocalReport() = LocalHazardReport(
    localId = localId,
    hazardType = hazardType,
    latitude = latitude,
    longitude = longitude,
    photoLocalPath = photoLocalPath,
    classificationLabel = classificationLabel,
    confidenceScore = confidenceScore,
    reportedAt = reportedAt,
    nhCorridor = nhCorridor,
    severity = severity
)
