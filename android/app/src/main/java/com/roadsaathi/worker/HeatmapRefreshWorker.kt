package com.roadsaathi.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roadsaathi.data.local.HeatmapCache
import com.roadsaathi.data.remote.ApiClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HeatmapRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiClient: ApiClient,
    private val heatmapCache: HeatmapCache
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val LAT_KEY = "lat"
        const val LNG_KEY = "lng"
        const val RADIUS_KEY = "radius"
        private const val DEFAULT_RADIUS = 5000.0
    }

    override suspend fun doWork(): Result {
        val lat = inputData.getDouble(LAT_KEY, 0.0)
        val lng = inputData.getDouble(LNG_KEY, 0.0)
        val radius = inputData.getDouble(RADIUS_KEY, DEFAULT_RADIUS)

        if (lat == 0.0 && lng == 0.0) {
            return Result.failure()
        }

        return try {
            val response = apiClient.api.getHeatmap(lat, lng, radius)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    heatmapCache.put(body, lat, lng, radius)
                    Result.success()
                } else {
                    Result.failure()
                }
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
