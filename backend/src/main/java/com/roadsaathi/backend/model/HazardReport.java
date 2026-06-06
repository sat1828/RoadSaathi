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
@Table(name = "hazard_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HazardReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "hazard_type", nullable = false, length = 32)
    private String hazardType;

    @Column
    private Float confidence;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column
    @Builder.Default
    private Integer severity = 1;

    @Column
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "nh_corridor")
    private String nhCorridor;

    @Column(name = "assigned_engineer_id")
    private UUID assignedEngineerId;

    @Column(name = "reported_at")
    private OffsetDateTime reportedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "confirm_count")
    @Builder.Default
    private Integer confirmCount = 0;

    @Column(name = "ai_brief", length = 500)
    private String aiBrief;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (reportedAt == null) {
            reportedAt = OffsetDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = OffsetDateTime.now().plusHours(48);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
