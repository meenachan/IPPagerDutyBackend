package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organization.id = :orgId AND (:entityType IS NULL OR ae.entityType = :entityType) AND (:entityId IS NULL OR ae.entityId = :entityId)")
    Page<AuditEvent> findByOrganizationAndFilters(@Param("orgId") UUID orgId,
                                                   @Param("entityType") String entityType,
                                                   @Param("entityId") UUID entityId,
                                                   Pageable pageable);

    Optional<AuditEvent> findTopByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(UUID orgId, String entityType, UUID entityId);
}
