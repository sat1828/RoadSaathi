package com.roadsaathi.backend.controller;

import com.roadsaathi.backend.dto.AssignRequest;
import com.roadsaathi.backend.dto.HazardReportResponse;
import com.roadsaathi.backend.dto.StatusUpdateRequest;
import com.roadsaathi.backend.service.HazardReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('NHAI_OFFICER')")
public class AdminController {

    private final HazardReportService hazardReportService;

    @GetMapping("/triage")
    public ResponseEntity<List<HazardReportResponse>> getTriageQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String hazardType,
            @RequestParam(required = false) Integer severity) {
        List<HazardReportResponse> queue = hazardReportService.getTriageQueue(page, limit, status, hazardType, severity);
        return ResponseEntity.ok(queue);
    }

    @PostMapping("/reports/{id}/assign")
    public ResponseEntity<Void> assignReport(@PathVariable UUID id,
                                              @Valid @RequestBody AssignRequest request) {
        hazardReportService.assignReport(id, request.getEngineerId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reports/{id}/status")
    public ResponseEntity<Void> updateReportStatus(@PathVariable UUID id,
                                                    @Valid @RequestBody StatusUpdateRequest request) {
        hazardReportService.updateReportStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
