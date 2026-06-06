package com.roadsaathi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverSessionRequest {

    @NotBlank
    private String fcmToken;

    private Double latitude;
    private Double longitude;
    private Float heading;
    private Float speedKmh;
}
