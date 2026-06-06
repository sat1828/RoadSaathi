package com.roadsaathi.backend.controller;

import com.roadsaathi.backend.dto.HazardReportResponse;
import com.roadsaathi.backend.dto.HeatmapResponse;
import com.roadsaathi.backend.service.HazardReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hazards")
@RequiredArgsConstructor
public class HazardController {

    private final HazardReportService hazardReportService;

    @GetMapping
    public ResponseEntity<List<HazardReportResponse>> getHazards(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String hazardType,
            @RequestParam(required = false) Integer severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit) {
        List<HazardReportResponse> reports = hazardReportService.getTriageQueue(page, limit, status, hazardType, severity);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") double radius) {
        HeatmapResponse response = hazardReportService.getReports(lat, lng, radius);
        return ResponseEntity.ok(response);
    }
}
