package com.roadsaathi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HazardReportRequest {

    @NotNull
    private String hazardType;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    private String classificationLabel;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double confidenceScore;

    private String reportedAt;

    private String photoUrl;

    private String nhCorridor;

    private Integer severity;
}
