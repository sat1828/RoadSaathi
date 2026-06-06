package com.roadsaathi.backend.controller;

import com.roadsaathi.backend.config.RateLimitingConfig;
import com.roadsaathi.backend.dto.HazardReportRequest;
import com.roadsaathi.backend.dto.HazardReportResponse;
import com.roadsaathi.backend.dto.HeatmapResponse;
import com.roadsaathi.backend.service.HazardReportService;
import com.roadsaathi.backend.service.S3Service;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

record S3UploadResponse(String url, String key) {}

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class HazardReportController {

    private final HazardReportService hazardReportService;
    private final S3Service s3Service;
    private final RateLimitingConfig rateLimitingConfig;

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestPart("request") HazardReportRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        Bucket bucket = rateLimitingConfig.resolveBucket(userId.toString());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Rate limit exceeded",
                            "retryAfterSeconds", waitSeconds
                    ));
        }

        HazardReportResponse response = hazardReportService.createReport(request, photo, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<HeatmapResponse> getReports(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") double radius) {
        HeatmapResponse response = hazardReportService.getReports(lat, lng, radius);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HazardReportResponse> getReport(@PathVariable UUID id) {
        HazardReportResponse response = hazardReportService.getReportById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmReport(@PathVariable UUID id) {
        hazardReportService.confirmReport(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismissReport(@PathVariable UUID id) {
        hazardReportService.dismissReport(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/s3-upload-url")
    public ResponseEntity<S3UploadResponse> getS3UploadUrl(
            @RequestParam(defaultValue = "photo.jpg") String fileName,
            @RequestParam(defaultValue = "image/jpeg") String contentType) {
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "jpg";
        String key = s3Service.generateKey("uploads", extension);
        String url = s3Service.generatePresignedPutUrl(key, contentType);
        return ResponseEntity.ok(new S3UploadResponse(url, key));
    }
}
