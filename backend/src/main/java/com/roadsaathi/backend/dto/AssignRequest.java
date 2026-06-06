package com.roadsaathi.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignRequest {

    @NotNull
    private UUID engineerId;
}
