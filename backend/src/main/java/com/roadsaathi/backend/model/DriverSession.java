package com.roadsaathi.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    @Column(name = "current_pos", columnDefinition = "geography(Point,4326)")
    private Point currentPos;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column
    private Float heading;

    @Column(name = "speed_kmh")
    private Float speedKmh;

    @PrePersist
    protected void onCreate() {
        if (lastSeen == null) {
            lastSeen = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeen = OffsetDateTime.now();
    }
}
