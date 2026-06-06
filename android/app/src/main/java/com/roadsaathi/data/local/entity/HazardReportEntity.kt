package com.roadsaathi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roadsaathi.data.local.SyncStatus

@Entity(tableName = "hazard_reports")
data class HazardReportEntity(
    @PrimaryKey
    val localId: String,
    val hazardType: String,
    val latitude: Double,
    val longitude: Double,
    val photoLocalPath: String? = null,
    val classificationLabel: String,
    val confidenceScore: Float,
    val reportedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val remoteId: String? = null,
    val nhCorridor: String? = null,
    val severity: Int = 1
)
