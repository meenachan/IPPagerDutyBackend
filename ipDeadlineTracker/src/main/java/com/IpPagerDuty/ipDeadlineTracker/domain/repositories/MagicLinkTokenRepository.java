package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, UUID> {
    Optional<MagicLinkToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE MagicLinkToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL AND t.expiresAt > :now")
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);
}
