package com.roadsaathi.domain.model

data class HeatmapCluster(
    val id: String,
    val hazardType: String,
    val count: Int,
    val latitude: Double,
    val longitude: Double,
    val aiBrief: String?,
    val severity: Int,
    val nhCorridor: String?
)
