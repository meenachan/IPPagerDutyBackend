package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
