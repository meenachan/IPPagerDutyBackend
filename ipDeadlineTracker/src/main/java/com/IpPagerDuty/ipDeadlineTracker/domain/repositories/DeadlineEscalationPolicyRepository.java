package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.DeadlineEscalationPolicy;
import com.IpPagerDuty.ipDeadlineTracker.domain.DeadlineEscalationPolicyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeadlineEscalationPolicyRepository extends JpaRepository<DeadlineEscalationPolicy, DeadlineEscalationPolicyId> {
    List<DeadlineEscalationPolicy> findByDeadlineId(UUID deadlineId);
    boolean existsByEscalationPolicyId(UUID escalationPolicyId);
}
