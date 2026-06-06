package com.roadsaathi.backend.service;

import com.roadsaathi.backend.dto.DriverSessionRequest;
import com.roadsaathi.backend.model.DriverSession;
import com.roadsaathi.backend.repository.DriverSessionRepository;
import com.roadsaathi.backend.repository.HazardReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverSessionService {

    private static final int SRID = 4326;

    private final DriverSessionRepository driverSessionRepository;
    private final HazardReportRepository hazardReportRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

    @Transactional
    public void upsertSession(DriverSessionRequest request, UUID userId) {
        var existing = driverSessionRepository.findByUserId(userId);

        DriverSession session = existing.orElseGet(() -> DriverSession.builder()
                .userId(userId)
                .build());

        session.setFcmToken(request.getFcmToken());
        session.setLastSeen(OffsetDateTime.now());
        session.setHeading(request.getHeading());
        session.setSpeedKmh(request.getSpeedKmh());

        if (request.getLatitude() != null && request.getLongitude() != null) {
            var point = geometryFactory.createPoint(
                    new Coordinate(request.getLongitude(), request.getLatitude())
            );
            session.setCurrentPos(point);
        }

        driverSessionRepository.save(session);
    }

    @Transactional
    public void purgeOldSessions() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(1);
        int deleted = driverSessionRepository.deleteByLastSeenBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} stale driver sessions", deleted);
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void scheduledPurge() {
        purgeOldSessions();
    }

    public List<String> findNearbyDrivers(double lat, double lng, double radiusMeters) {
        return hazardReportRepository.findNearbyFcmTokens(
                lat, lng, radiusMeters, OffsetDateTime.now().minusHours(1)
        );
    }
}
