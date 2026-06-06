package com.roadsaathi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HeatmapResponse(
    @SerializedName("type")
    val type: String = "FeatureCollection",
    @SerializedName("features")
    val features: List<HeatmapFeature>
)

data class HeatmapFeature(
    @SerializedName("type")
    val type: String = "Feature",
    @SerializedName("geometry")
    val geometry: HeatmapGeometry,
    @SerializedName("properties")
    val properties: HeatmapProperties
)

data class HeatmapGeometry(
    @SerializedName("type")
    val type: String = "Point",
    @SerializedName("coordinates")
    val coordinates: List<Double>
)

data class HeatmapProperties(
    @SerializedName("cluster_id")
    val clusterId: String,
    @SerializedName("hazard_type")
    val hazardType: String,
    @SerializedName("count")
    val count: Int,
    @SerializedName("ai_brief")
    val aiBrief: String? = null,
    @SerializedName("severity")
    val severity: Int = 1,
    @SerializedName("nh_corridor")
    val nhCorridor: String? = null
)
