package com.roadsaathi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HazardReportResponse {

    private UUID id;
    private String hazardType;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String classificationLabel;
    private Double confidenceScore;
    private OffsetDateTime reportedAt;
    private String nhCorridor;
    private Integer severity;
    private String status;
    private OffsetDateTime expiresAt;
    private Integer confirmCount;
    private String aiBrief;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
