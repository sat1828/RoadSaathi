package com.roadsaathi.backend.repository;

import com.roadsaathi.backend.model.HazardReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HazardReportRepository extends JpaRepository<HazardReport, UUID> {

    List<HazardReport> findByStatusAndExpiresAtAfter(String status, OffsetDateTime now);

    List<HazardReport> findByStatus(String status);

    Page<HazardReport> findByStatus(String status, Pageable pageable);

    int countByStatusAndReporterIdAndReportedAtAfter(String status, UUID reporterId, OffsetDateTime after);

    long countByStatus(String status);

    long countByStatusNot(String status);

    long countByStatusAndReportedAtAfter(String status, OffsetDateTime after);

    @Query("SELECT COUNT(DISTINCT hr.reporterId) FROM HazardReport hr WHERE hr.status <> 'EXPIRED'")
    long countDistinctActiveReporters();

    @Query("SELECT COUNT(DISTINCT hr.nhCorridor) FROM HazardReport hr WHERE hr.status <> 'EXPIRED' AND hr.nhCorridor IS NOT NULL")
    long countDistinctActiveCorridors();

    @Query("SELECT hr.hazardType, COUNT(hr) FROM HazardReport hr WHERE hr.status <> 'EXPIRED' GROUP BY hr.hazardType")
    List<Object[]> countByHazardType();

    @Query("SELECT hr.status, COUNT(hr) FROM HazardReport hr GROUP BY hr.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT FUNCTION('DATE', hr.reportedAt) as date, COUNT(hr) FROM HazardReport hr WHERE hr.reportedAt >= :since GROUP BY FUNCTION('DATE', hr.reportedAt) ORDER BY date")
    List<Object[]> dailyReportCount(@Param("since") OffsetDateTime since);

    @Query("SELECT hr.nhCorridor, COUNT(hr) FROM HazardReport hr WHERE hr.nhCorridor IS NOT NULL AND hr.status <> 'EXPIRED' GROUP BY hr.nhCorridor ORDER BY COUNT(hr) DESC")
    List<Object[]> countByCorridor();

    @Query(value = "SELECT ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng, hazard_type, COUNT(*) as cnt, AVG(severity) as avg_sev FROM hazard_reports WHERE status <> 'EXPIRED' GROUP BY lat, lng, hazard_type HAVING COUNT(*) >= 3 ORDER BY cnt DESC LIMIT 20", nativeQuery = true)
    List<Object[]> findBlackspots();

    @Query(value = "SELECT * FROM hazard_reports WHERE status = 'ACTIVE' AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters) AND expires_at > NOW() ORDER BY ST_Distance(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)", nativeQuery = true)
    List<HazardReport> findActiveReportsWithinRadius(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") double radiusMeters);

    @Query(value = "SELECT cluster_id, ST_AsGeoJSON(ST_Centroid(ST_Collect(location::geometry))) as center, ST_Collect(location::geometry) as geom, array_agg(hazard_type) as types, array_agg(confidence) as confidences, array_agg(id::text) as report_ids FROM (SELECT ST_ClusterDBSCAN(location::geography, 5000, 3) OVER () AS cluster_id, * FROM hazard_reports WHERE status = 'ACTIVE' AND expires_at > NOW()) sub WHERE cluster_id IS NOT NULL GROUP BY cluster_id", nativeQuery = true)
    List<Object[]> findClusters();

    @Query(value = "SELECT * FROM hazard_reports WHERE ST_DWithin(location, ST_GeomFromText(:routeWkt, 4326)::geography, :bufferMeters) AND status = 'ACTIVE'", nativeQuery = true)
    List<HazardReport> findHazardsOnRoute(@Param("routeWkt") String routeWkt, @Param("bufferMeters") double bufferMeters);

    @Query(value = "SELECT ds.fcm_token FROM driver_sessions ds WHERE ST_DWithin(ds.current_pos, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters) AND ds.last_seen > :since", nativeQuery = true)
    List<String> findNearbyFcmTokens(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") double radiusMeters, @Param("since") OffsetDateTime since);

    @Modifying
    @Query("UPDATE HazardReport hr SET hr.status = 'EXPIRED', hr.updatedAt = CURRENT_TIMESTAMP WHERE hr.status = 'ACTIVE' AND hr.expiresAt < CURRENT_TIMESTAMP AND hr.confirmCount = 0")
    int expireActiveReports();
}
