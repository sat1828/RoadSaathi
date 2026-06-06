package com.roadsaathi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private List<HazardTypeCount> byType;
    private List<StatusCount> byStatus;
    private List<DailyTrend> dailyTrend;
    private List<CorridorCount> byCorridor;
    private List<BlackspotResponse> blackspots;
    private Map<String, Double> severityDistribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HazardTypeCount {
        private String hazardType;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusCount {
        private String status;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTrend {
        private String date;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CorridorCount {
        private String corridor;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlackspotResponse {
        private double lat;
        private double lng;
        private String hazardType;
        private long count;
        private double avgSeverity;
    }
}
