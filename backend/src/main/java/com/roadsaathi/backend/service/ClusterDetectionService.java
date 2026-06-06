package com.roadsaathi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadsaathi.backend.repository.HazardReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterDetectionService {

    private final HazardReportRepository hazardReportRepository;
    private final ClaudeAIService claudeAIService;
    private final FCMMessagingService fcmMessagingService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void detectAndNotify() {
        log.info("Running cluster detection...");
        try {
            var clusters = hazardReportRepository.findClusters();

            for (Object[] cluster : clusters) {
                Integer clusterId = (Integer) cluster[0];
                String[] types = (String[]) cluster[3];
                Float[] confidences = (Float[]) cluster[4];
                String[] reportIds = (String[]) cluster[5];

                if (reportIds == null || reportIds.length < 3) {
                    continue;
                }

                String hazardType = (types != null && types.length > 0) ? types[0] : "UNKNOWN";

                ClaudeAIService.ClusterData clusterData = new ClaudeAIService.ClusterData();
                ClaudeAIService.ReportSummary summary = new ClaudeAIService.ReportSummary();
                summary.setType(hazardType);
                summary.setCount(reportIds.length);
                summary.setConfidence(confidences != null && confidences.length > 0 ? confidences[0] : null);
                clusterData.setReports(Arrays.asList(summary));
                clusterData.setFirstReported(OffsetDateTime.now().toString());

                String aiBrief = claudeAIService.generateHazardBrief(clusterData);
                log.info("Cluster {} brief: {}", clusterId, aiBrief);

                double centerLat = 0;
                double centerLng = 0;
                try {
                    String geoJson = (String) cluster[1];
                    var node = objectMapper.readTree(geoJson);
                    centerLng = node.get("coordinates").get(0).asDouble();
                    centerLat = node.get("coordinates").get(1).asDouble();
                } catch (Exception e) {
                    log.error("Failed to parse cluster center", e);
                    continue;
                }

                var fcmTokens = hazardReportRepository.findNearbyFcmTokens(
                        centerLat, centerLng, 10000, OffsetDateTime.now().minusHours(1)
                );

                if (!fcmTokens.isEmpty()) {
                    fcmMessagingService.sendHazardAlert(
                            fcmTokens,
                            clusterId.toString(),
                            hazardType,
                            0,
                            aiBrief,
                            centerLat,
                            centerLng
                    );
                }
            }

            log.info("Cluster detection completed. Found {} clusters.", clusters.size());
        } catch (Exception e) {
            log.error("Error during cluster detection", e);
        }
    }
}
