package com.roadsaathi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.roadsaathi.data.local.SyncStatus
import com.roadsaathi.data.local.entity.HazardReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HazardReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: HazardReportEntity)

    @Query("UPDATE hazard_reports SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: String, status: SyncStatus)

    @Query("SELECT * FROM hazard_reports WHERE syncStatus = 'PENDING'")
    fun getPendingReports(): Flow<List<HazardReportEntity>>

    @Query("SELECT * FROM hazard_reports WHERE syncStatus = :status")
    fun getReportsByStatus(status: SyncStatus): Flow<List<HazardReportEntity>>

    @Query("SELECT * FROM hazard_reports WHERE localId = :localId")
    fun getReportById(localId: String): Flow<HazardReportEntity?>

    @Query("SELECT * FROM hazard_reports ORDER BY reportedAt DESC")
    fun getAllReports(): Flow<List<HazardReportEntity>>

    @Query("SELECT * FROM hazard_reports WHERE syncStatus = 'PENDING'")
    suspend fun getPendingReportsOnce(): List<HazardReportEntity>

    @Query("SELECT COUNT(*) FROM hazard_reports WHERE syncStatus = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("UPDATE hazard_reports SET remoteId = :remoteId WHERE localId = :localId")
    suspend fun updateRemoteId(localId: String, remoteId: String)

    @Query("DELETE FROM hazard_reports WHERE localId = :localId")
    suspend fun deleteReport(localId: String)

    @Query("DELETE FROM hazard_reports")
    suspend fun deleteAll()
}
