package com.roadsaathi.domain.model

import com.roadsaathi.data.local.SyncStatus
import com.roadsaathi.data.local.entity.HazardReportEntity

data class HazardReport(
    val localId: String,
    val hazardType: String,
    val latitude: Double,
    val longitude: Double,
    val photoLocalPath: String?,
    val classificationLabel: String,
    val confidenceScore: Float,
    val reportedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
    val nhCorridor: String?,
    val severity: Int
) {
    companion object {
        fun fromEntity(entity: HazardReportEntity): HazardReport {
            return HazardReport(
                localId = entity.localId,
                hazardType = entity.hazardType,
                latitude = entity.latitude,
                longitude = entity.longitude,
                photoLocalPath = entity.photoLocalPath,
                classificationLabel = entity.classificationLabel,
                confidenceScore = entity.confidenceScore,
                reportedAt = entity.reportedAt,
                syncStatus = entity.syncStatus,
                remoteId = entity.remoteId,
                nhCorridor = entity.nhCorridor,
                severity = entity.severity
            )
        }

        fun toEntity(domain: HazardReport): HazardReportEntity {
            return HazardReportEntity(
                localId = domain.localId,
                hazardType = domain.hazardType,
                latitude = domain.latitude,
                longitude = domain.longitude,
                photoLocalPath = domain.photoLocalPath,
                classificationLabel = domain.classificationLabel,
                confidenceScore = domain.confidenceScore,
                reportedAt = domain.reportedAt,
                syncStatus = domain.syncStatus,
                remoteId = domain.remoteId,
                nhCorridor = domain.nhCorridor,
                severity = domain.severity
            )
        }
    }
}
