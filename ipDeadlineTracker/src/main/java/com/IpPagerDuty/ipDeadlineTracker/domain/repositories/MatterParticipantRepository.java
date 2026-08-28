package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.MatterParticipant;
import com.IpPagerDuty.ipDeadlineTracker.domain.MatterParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface MatterParticipantRepository extends JpaRepository<MatterParticipant, MatterParticipantId> {
    Set<MatterParticipant> findByMatterId(UUID matterId);
}
