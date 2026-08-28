package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.Matter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatterRepository extends JpaRepository<Matter, UUID> {
    @Query("""
        SELECT m FROM Matter m
        LEFT JOIN FETCH m.participants p
        WHERE m.organization.id = :orgId
        ORDER BY m.createdAt DESC
        """)
    List<Matter> findByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT m FROM Matter m LEFT JOIN FETCH m.participants WHERE m.id = :id")
    Optional<Matter> findByIdWithParticipants(@Param("id") UUID id);
}
