package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationPolicyRepository extends JpaRepository<EscalationPolicy, UUID> {
    List<EscalationPolicy> findByOrganizationId(UUID organizationId);
    List<EscalationPolicy> findByEmailGroupId(UUID emailGroupId);
}
