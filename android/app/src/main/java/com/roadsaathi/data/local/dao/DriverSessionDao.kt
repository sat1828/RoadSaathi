package com.roadsaathi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.roadsaathi.data.local.entity.DriverSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverSessionDao {

    @Upsert
    suspend fun upsertSession(session: DriverSessionEntity)

    @Query("SELECT * FROM driver_sessions LIMIT 1")
    fun getSession(): Flow<DriverSessionEntity?>

    @Query("DELETE FROM driver_sessions")
    suspend fun deleteSession()
}
