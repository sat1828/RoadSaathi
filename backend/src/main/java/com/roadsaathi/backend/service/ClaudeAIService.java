package com.roadsaathi.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.claude.api-key}")
    private String apiKey;

    @Value("${app.claude.api-url}")
    private String apiUrl;

    @Value("${app.claude.model}")
    private String model;

    @Value("${app.claude.max-tokens}")
    private int maxTokens;

    public String generateHazardBrief(ClusterData clusterData) {
        String prompt = buildPrompt(clusterData);

        try {
            String requestBody = objectMapper.writeValueAsString(
                    new ClaudeRequest(model, maxTokens, List.of(
                            new ClaudeMessage("user", prompt)
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            String responseBody = response.getBody();

            if (responseBody == null) {
                return "Unable to generate hazard brief at this time.";
            }

            JsonNode responseJson = objectMapper.readTree(responseBody);
            String text = responseJson.path("content")
                    .get(0)
                    .path("text")
                    .asText();

            return truncateTo60Words(text);

        } catch (Exception e) {
            log.error("Failed to call Claude API", e);
            return "Hazard cluster detected. Please exercise caution in this area.";
        }
    }

    private String buildPrompt(ClusterData clusterData) {
        return String.format(
                "You are a road safety analyst. Analyze this cluster of road hazards and provide a brief assessment (max 60 words):\n\n" +
                        "Corridor: %s\n" +
                        "Number of reports: %d\n" +
                        "Hazard types: %s\n" +
                        "First reported: %s\n\n" +
                        "Provide a concise safety assessment for drivers approaching this area.",
                clusterData.getNhCorridor() != null ? clusterData.getNhCorridor() : "Unknown",
                clusterData.getReports().stream().mapToInt(ReportSummary::getCount).sum(),
                clusterData.getReports().stream()
                        .map(r -> r.getType() + "(" + r.getCount() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Unknown"),
                clusterData.getFirstReported()
        );
    }

    private String truncateTo60Words(String text) {
        String[] words = text.split("\\s+");
        if (words.length <= 60) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            if (i > 0) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString();
    }

    @Data
    public static class ClusterData {
        private String nhCorridor;
        private List<ReportSummary> reports;
        private String firstReported;
    }

    @Data
    public static class ReportSummary {
        private String type;
        private Float confidence;
        private int count;
    }

    @Data
    private static class ClaudeRequest {
        private final String model;
        private final int maxTokens;
        private final List<ClaudeMessage> messages;
    }

    @Data
    private static class ClaudeMessage {
        private final String role;
        private final String content;
    }
}
