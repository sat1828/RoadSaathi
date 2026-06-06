package com.roadsaathi.domain.usecase

import com.roadsaathi.data.remote.dto.HeatmapFeature
import com.roadsaathi.data.repository.HazardRepository
import com.roadsaathi.domain.model.HeatmapCluster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHeatmapDataUseCase @Inject constructor(
    private val hazardRepository: HazardRepository
) {
    operator fun invoke(lat: Double, lng: Double, radius: Double): Flow<Result<List<HeatmapCluster>>> {
        return hazardRepository.fetchHeatmapData(lat, lng, radius).map { result ->
            result.map { response ->
                response.features.map { feature ->
                    val coords = feature.geometry.coordinates
                    HeatmapCluster(
                        id = feature.properties.clusterId,
                        hazardType = feature.properties.hazardType,
                        count = feature.properties.count,
                        latitude = coords.getOrElse(1) { 0.0 },
                        longitude = coords.getOrElse(0) { 0.0 },
                        aiBrief = feature.properties.aiBrief,
                        severity = feature.properties.severity,
                        nhCorridor = feature.properties.nhCorridor
                    )
                }
            }
        }
    }
}
