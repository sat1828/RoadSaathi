package com.roadsaathi.backend.dto.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadsaathi.backend.dto.HeatmapResponse;
import com.roadsaathi.backend.dto.HeatmapResponse.FeatureDto;
import com.roadsaathi.backend.dto.HeatmapResponse.GeometryDto;
import com.roadsaathi.backend.dto.HeatmapResponse.PropertiesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HeatmapMapper {

    private final ObjectMapper objectMapper;

    public HeatmapResponse toHeatmapResponse(List<Object[]> clusterResults) {
        List<FeatureDto> features = new ArrayList<>();

        for (Object[] row : clusterResults) {
            Integer clusterId = (Integer) row[0];
            String centerGeoJson = (String) row[1];
            String[] types = (String[]) row[3];
            Float[] confidences = (Float[]) row[4];
            String[] reportIds = (String[]) row[5];

            try {
                JsonNode centerNode = objectMapper.readTree(centerGeoJson);
                double lon = centerNode.get("coordinates").get(0).asDouble();
                double lat = centerNode.get("coordinates").get(1).asDouble();

                String hazardType = (types != null && types.length > 0) ? types[0] : "UNKNOWN";
                Integer count = (reportIds != null) ? reportIds.length : 0;

                GeometryDto geometry = GeometryDto.builder()
                        .coordinates(List.of(lon, lat))
                        .build();

                PropertiesDto properties = PropertiesDto.builder()
                        .clusterId(clusterId.toString())
                        .hazardType(hazardType)
                        .count(count)
                        .build();

                features.add(FeatureDto.builder()
                        .geometry(geometry)
                        .properties(properties)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse cluster GeoJSON", e);
            }
        }

        return HeatmapResponse.builder()
                .features(features)
                .build();
    }
}
