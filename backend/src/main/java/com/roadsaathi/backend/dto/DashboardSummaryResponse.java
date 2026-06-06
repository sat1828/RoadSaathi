package com.roadsaathi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private long totalReports;
    private long activeReports;
    private long pendingReview;
    private long assigned;
    private long resolvedLastWeek;
    private double avgResponseTimeHours;
    private long uniqueDrivers;
    private long activeCorridors;
}
