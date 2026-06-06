package com.roadsaathi.backend.controller;

import com.roadsaathi.backend.dto.DashboardSummaryResponse;
import com.roadsaathi.backend.dto.AnalyticsResponse;
import com.roadsaathi.backend.service.HazardReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final HazardReportService hazardReportService;

    @GetMapping("/dashboard/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(hazardReportService.getDashboardSummary());
    }

    @GetMapping("/analytics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(hazardReportService.getAnalytics(period, from, to));
    }
}
