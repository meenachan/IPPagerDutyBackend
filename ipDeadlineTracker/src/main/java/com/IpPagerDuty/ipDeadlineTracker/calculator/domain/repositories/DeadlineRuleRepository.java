package com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadlineRuleRepository extends JpaRepository<DeadlineRule, UUID> {
    List<DeadlineRule> findByActiveTrue();

    List<DeadlineRule> findByCodeAndActiveTrue(String code);

    Optional<DeadlineRule> findByCodeAndOfficeCodeAndActiveTrue(String code, String officeCode);

    List<DeadlineRule> findByCode(String code);
}
