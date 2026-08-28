package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationEmailGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationEmailGroupRepository extends JpaRepository<EscalationEmailGroup, UUID> {
    List<EscalationEmailGroup> findByOrganizationId(UUID organizationId);
}
