package com.roadsaathi.backend.repository;

import com.roadsaathi.backend.model.DriverSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverSessionRepository extends JpaRepository<DriverSession, UUID> {

    Optional<DriverSession> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM DriverSession ds WHERE ds.lastSeen < :before")
    int deleteByLastSeenBefore(@Param("before") OffsetDateTime before);
}
