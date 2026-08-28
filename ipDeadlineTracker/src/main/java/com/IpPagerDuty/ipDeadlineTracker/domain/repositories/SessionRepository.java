package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE Session s SET s.expiresAt = :expiresAt WHERE s.id = :id AND s.expiresAt > :now")
    int extendExpiry(@Param("id") UUID id, @Param("expiresAt") Instant expiresAt, @Param("now") Instant now);
}
