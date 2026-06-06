package com.roadsaathi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatmapResponse {

    @Builder.Default
    private String type = "FeatureCollection";

    private List<FeatureDto> features;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeatureDto {
        @Builder.Default
        private String type = "Feature";
        private GeometryDto geometry;
        private PropertiesDto properties;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeometryDto {
        @Builder.Default
        private String type = "Point";
        private List<Double> coordinates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PropertiesDto {
        private String clusterId;
        private String hazardType;
        private Integer count;
        private String aiBrief;
        private Integer severity;
        private String nhCorridor;
    }
}
