package com.roadsaathi.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.roadsaathi.data.remote.dto.HeatmapResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeatmapCache @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("heatmap_cache", Context.MODE_PRIVATE)

    fun put(response: HeatmapResponse, lat: Double, lng: Double, radius: Double) {
        val json = gson.toJson(response)
        prefs.edit()
            .putString(KEY_DATA, json)
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LNG, lng.toFloat())
            .putFloat(KEY_RADIUS, radius.toFloat())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun get(): CachedHeatmap? {
        val json = prefs.getString(KEY_DATA, null) ?: return null
        val response = try {
            gson.fromJson(json, HeatmapResponse::class.java)
        } catch (e: Exception) {
            null
        } ?: return null
        return CachedHeatmap(
            response = response,
            lat = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            lng = prefs.getFloat(KEY_LNG, 0f).toDouble(),
            radius = prefs.getFloat(KEY_RADIUS, 0f).toDouble(),
            timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_DATA = "heatmap_data"
        private const val KEY_LAT = "heatmap_lat"
        private const val KEY_LNG = "heatmap_lng"
        private const val KEY_RADIUS = "heatmap_radius"
        private const val KEY_TIMESTAMP = "heatmap_timestamp"
    }
}

data class CachedHeatmap(
    val response: HeatmapResponse,
    val lat: Double,
    val lng: Double,
    val radius: Double,
    val timestamp: Long
)
