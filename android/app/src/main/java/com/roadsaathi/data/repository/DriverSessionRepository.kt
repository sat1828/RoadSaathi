package com.roadsaathi.data.repository

import com.roadsaathi.data.local.dao.DriverSessionDao
import com.roadsaathi.data.local.entity.DriverSessionEntity
import com.roadsaathi.data.remote.ApiClient
import com.roadsaathi.data.remote.dto.DriverSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverSessionRepository @Inject constructor(
    private val driverSessionDao: DriverSessionDao,
    private val apiClient: ApiClient
) {
    suspend fun upsertSession(fcmToken: String, lat: Double, lng: Double) {
        val session = DriverSessionEntity(
            fcmToken = fcmToken,
            lastLat = lat,
            lastLng = lng,
            lastSeen = System.currentTimeMillis()
        )
        driverSessionDao.upsertSession(session)
    }

    fun getSession(): Flow<DriverSessionEntity?> = driverSessionDao.getSession()

    suspend fun deleteSession() {
        driverSessionDao.deleteSession()
    }

    fun syncSession(fcmToken: String, lat: Double, lng: Double): Flow<Result<Unit>> = flow {
        try {
            upsertSession(fcmToken, lat, lng)
            val request = DriverSessionRequest(
                fcmToken = fcmToken,
                latitude = lat,
                longitude = lng
            )
            val response = apiClient.api.upsertSession(request)
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Session sync failed: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
