package com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.Calculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CalculationRepository extends JpaRepository<Calculation, UUID> {
}
