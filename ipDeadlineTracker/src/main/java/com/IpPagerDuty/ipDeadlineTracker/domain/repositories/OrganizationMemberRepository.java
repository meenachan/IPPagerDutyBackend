package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByUserId(UUID userId);
    Optional<OrganizationMember> findByUserEmail(String email);
    List<OrganizationMember> findByOrganizationId(UUID organizationId);
    boolean existsByOrganizationIdAndUserEmail(UUID organizationId, String email);

    @Query("SELECT om FROM OrganizationMember om WHERE om.organization.id = :orgId AND om.role IN ('BUSINESS_OWNER', 'LAWYER', 'PARALEGAL') ORDER BY om.createdAt")
    List<OrganizationMember> findStaffByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT om FROM OrganizationMember om WHERE om.organization.id = :orgId AND om.role = 'BUSINESS_OWNER' ORDER BY om.createdAt")
    List<OrganizationMember> findOwnersByOrganizationId(@Param("orgId") UUID orgId);
}
