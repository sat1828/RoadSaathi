package com.roadsaathi.backend.service;

import com.roadsaathi.backend.dto.AnalyticsResponse;
import com.roadsaathi.backend.dto.DashboardSummaryResponse;
import com.roadsaathi.backend.dto.HazardReportRequest;
import com.roadsaathi.backend.dto.HazardReportResponse;
import com.roadsaathi.backend.dto.HeatmapResponse;
import com.roadsaathi.backend.dto.mapper.HazardReportMapper;
import com.roadsaathi.backend.dto.mapper.HeatmapMapper;
import com.roadsaathi.backend.exception.ResourceNotFoundException;
import com.roadsaathi.backend.model.HazardReport;
import com.roadsaathi.backend.repository.HazardReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HazardReportService {

    private static final int SRID = 4326;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/heic"
    );
    private static final long MAX_FILE_SIZE = 8 * 1024 * 1024;

    private final HazardReportRepository hazardReportRepository;
    private final HazardReportMapper hazardReportMapper;
    private final HeatmapMapper heatmapMapper;
    private final S3Service s3Service;
    private final ClusterDetectionService clusterDetectionService;
    private final FCMMessagingService fcmMessagingService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        long totalReports = hazardReportRepository.count();
        long activeReports = hazardReportRepository.countByStatus("ACTIVE");
        long pendingReview = hazardReportRepository.countByStatus("ACTIVE");
        long assigned = hazardReportRepository.countByStatus("ASSIGNED");
        long resolvedLastWeek = hazardReportRepository.countByStatusAndReportedAtAfter(
                "RESOLVED", OffsetDateTime.now().minusDays(7));
        long uniqueDrivers = hazardReportRepository.countDistinctActiveReporters();
        long activeCorridors = hazardReportRepository.countDistinctActiveCorridors();

        return DashboardSummaryResponse.builder()
                .totalReports(totalReports)
                .activeReports(activeReports)
                .pendingReview(pendingReview)
                .assigned(assigned)
                .resolvedLastWeek(resolvedLastWeek)
                .avgResponseTimeHours(4.5)
                .uniqueDrivers(uniqueDrivers)
                .activeCorridors(activeCorridors)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String period, LocalDate from, LocalDate to) {
        OffsetDateTime since;
        if (from != null) {
            since = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        } else if ("week".equals(period)) {
            since = OffsetDateTime.now().minusDays(7);
        } else if ("month".equals(period)) {
            since = OffsetDateTime.now().minusDays(30);
        } else if ("year".equals(period)) {
            since = OffsetDateTime.now().minusDays(365);
        } else {
            since = OffsetDateTime.now().minusDays(30);
        }

        List<AnalyticsResponse.HazardTypeCount> byType = hazardReportRepository.countByHazardType()
                .stream().map(r -> AnalyticsResponse.HazardTypeCount.builder()
                        .hazardType((String) r[0])
                        .count((Long) r[1])
                        .build())
                .collect(Collectors.toList());

        List<AnalyticsResponse.StatusCount> byStatus = hazardReportRepository.countByStatusGrouped()
                .stream().map(r -> AnalyticsResponse.StatusCount.builder()
                        .status((String) r[0])
                        .count((Long) r[1])
                        .build())
                .collect(Collectors.toList());

        List<AnalyticsResponse.DailyTrend> dailyTrend = hazardReportRepository.dailyReportCount(since)
                .stream().map(r -> {
                    java.sql.Date date = (java.sql.Date) r[0];
                    return AnalyticsResponse.DailyTrend.builder()
                            .date(date.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .count((Long) r[1])
                            .build();
                })
                .collect(Collectors.toList());

        List<AnalyticsResponse.CorridorCount> byCorridor = hazardReportRepository.countByCorridor()
                .stream().map(r -> AnalyticsResponse.CorridorCount.builder()
                        .corridor((String) r[0])
                        .count((Long) r[1])
                        .build())
                .collect(Collectors.toList());

        List<AnalyticsResponse.BlackspotResponse> blackspots = hazardReportRepository.findBlackspots()
                .stream().map(r -> AnalyticsResponse.BlackspotResponse.builder()
                        .lat((Double) r[0])
                        .lng((Double) r[1])
                        .hazardType((String) r[2])
                        .count((Long) r[3])
                        .avgSeverity(((Number) r[4]).doubleValue())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .byType(byType)
                .byStatus(byStatus)
                .dailyTrend(dailyTrend)
                .byCorridor(byCorridor)
                .blackspots(blackspots)
                .build();
    }

    @Transactional
    public HazardReportResponse createReport(HazardReportRequest request, MultipartFile photo, UUID reporterId) {
        String photoUrl = request.getPhotoUrl();

        if (photoUrl == null && photo != null && !photo.isEmpty()) {
            validatePhoto(photo);
            try {
                String extension = extractExtension(photo.getContentType());
                String key = s3Service.generateKey("reports", extension);
                photoUrl = s3Service.uploadPhoto(key, photo.getInputStream(), photo.getContentType());
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload photo", e);
            }
        }

        var point = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );

        OffsetDateTime reportedAt = request.getReportedAt() != null
                ? OffsetDateTime.parse(request.getReportedAt())
                : OffsetDateTime.now();

        HazardReport report = HazardReport.builder()
                .reporterId(reporterId)
                .hazardType(request.getHazardType())
                .confidence(request.getConfidenceScore() != null ? request.getConfidenceScore().floatValue() : null)
                .location(point)
                .photoUrl(photoUrl)
                .severity(request.getSeverity() != null ? request.getSeverity() : 1)
                .status("ACTIVE")
                .nhCorridor(request.getNhCorridor())
                .reportedAt(reportedAt)
                .expiresAt(reportedAt.plusHours(48))
                .confirmCount(0)
                .build();

        report = hazardReportRepository.save(report);

        try {
            clusterDetectionService.detectAndNotify();
        } catch (Exception e) {
            log.warn("Cluster detection triggered but failed (non-blocking): {}", e.getMessage());
        }

        var response = hazardReportMapper.toResponse(report);
        response.setClassificationLabel(request.getClassificationLabel());
        response.setConfidenceScore(request.getConfidenceScore());

        return response;
    }

    @Transactional(readOnly = true)
    public HeatmapResponse getReports(double lat, double lng, double radius) {
        var reports = hazardReportRepository.findActiveReportsWithinRadius(lat, lng, radius);
        var clusters = hazardReportRepository.findClusters();

        if (!clusters.isEmpty()) {
            return heatmapMapper.toHeatmapResponse(clusters);
        }

        var features = reports.stream().map(report -> {
            var response = hazardReportMapper.toResponse(report);
            return HeatmapResponse.FeatureDto.builder()
                    .geometry(HeatmapResponse.GeometryDto.builder()
                            .coordinates(List.of(response.getLongitude(), response.getLatitude()))
                            .build())
                    .properties(HeatmapResponse.PropertiesDto.builder()
                            .hazardType(response.getHazardType())
                            .count(1)
                            .severity(response.getSeverity())
                            .nhCorridor(response.getNhCorridor())
                            .aiBrief(response.getAiBrief())
                            .build())
                    .build();
        }).collect(Collectors.toList());

        return HeatmapResponse.builder()
                .features(features)
                .build();
    }

    @Transactional(readOnly = true)
    public HazardReportResponse getReportById(UUID id) {
        var report = hazardReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard report not found: " + id));
        return hazardReportMapper.toResponse(report);
    }

    @Transactional
    public void confirmReport(UUID id) {
        var report = hazardReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard report not found: " + id));

        report.setConfirmCount(report.getConfirmCount() + 1);
        report.setExpiresAt(report.getExpiresAt().plusHours(24));

        if (report.getConfirmCount() >= 1) {
            report.setStatus("CONFIRMED");
        }

        hazardReportRepository.save(report);
    }

    @Transactional
    public void dismissReport(UUID id) {
        var report = hazardReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard report not found: " + id));

        report.setStatus("UNCONFIRMED");
        hazardReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<HazardReportResponse> getTriageQueue(int page, int limit, String status, String hazardType, Integer severity) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "severity").and(Sort.by(Sort.Direction.ASC, "reportedAt")));

        String effectiveStatus = (status != null) ? status : "ACTIVE";
        Page<HazardReport> reportsPage;

        if (hazardType != null || severity != null) {
            List<HazardReport> all = hazardReportRepository.findByStatus(effectiveStatus);
            var filtered = all.stream()
                    .filter(r -> hazardType == null || hazardType.equalsIgnoreCase(r.getHazardType()))
                    .filter(r -> severity == null || severity.equals(r.getSeverity()))
                    .sorted((a, b) -> {
                        int sevCompare = Integer.compare(
                                b.getSeverity() != null ? b.getSeverity() : 0,
                                a.getSeverity() != null ? a.getSeverity() : 0
                        );
                        if (sevCompare != 0) return sevCompare;
                        return a.getReportedAt().compareTo(b.getReportedAt());
                    })
                    .collect(Collectors.toList());
            return filtered.stream()
                    .skip((long) page * limit)
                    .limit(limit)
                    .map(hazardReportMapper::toResponse)
                    .collect(Collectors.toList());
        }

        reportsPage = hazardReportRepository.findByStatus(effectiveStatus, pageable);
        return reportsPage.stream()
                .map(hazardReportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignReport(UUID id, UUID engineerId) {
        var report = hazardReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard report not found: " + id));
        report.setStatus("ASSIGNED");
        report.setAssignedEngineerId(engineerId);
        hazardReportRepository.save(report);
    }

    @Transactional
    public void updateReportStatus(UUID id, String status) {
        var report = hazardReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard report not found: " + id));
        report.setStatus(status);
        hazardReportRepository.save(report);
    }

    private void validatePhoto(MultipartFile photo) {
        String contentType = photo.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid photo format. Allowed: JPEG, PNG, WebP, HEIC");
        }
        if (photo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Photo size exceeds 8MB limit");
        }
    }

    private String extractExtension(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            default -> "jpg";
        };
    }
}
