package com.roadsaathi.backend.controller;

import com.roadsaathi.backend.dto.DriverSessionRequest;
import com.roadsaathi.backend.service.DriverSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverSessionController {

    private final DriverSessionService driverSessionService;

    @PostMapping("/session")
    public ResponseEntity<Void> upsertSession(@Valid @RequestBody DriverSessionRequest request,
                                              Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        driverSessionService.upsertSession(request, userId);
        return ResponseEntity.ok().build();
    }
}
