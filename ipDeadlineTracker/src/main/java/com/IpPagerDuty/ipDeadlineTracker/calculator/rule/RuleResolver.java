package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories.DeadlineRuleRepository;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the single active, effective DeadlineRule for a given code
 * (+ optional office/chapter). Never guesses or falls back silently —
 * an unknown or inactive rule is always a rejected calculation
 * (spec section 15).
 */
@Component
public class RuleResolver {
    private final DeadlineRuleRepository ruleRepository;

    public RuleResolver(DeadlineRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public DeadlineRule resolve(String ruleCode, String officeCode, LocalDate asOf) {
        Optional<DeadlineRule> match;
        if (officeCode != null) {
            match = ruleRepository.findByCodeAndOfficeCodeAndActiveTrue(ruleCode, officeCode);
        } else {
            match = ruleRepository.findByCodeAndActiveTrue(ruleCode).stream()
                .filter(r -> r.getOfficeCode() == null)
                .findFirst();
        }

        DeadlineRule rule = match.orElseThrow(() -> new BadRequestException(
            "No verified calculation rule is available",
            Map.of("ruleCode", ruleCode, "officeCode", officeCode == null ? "" : officeCode)));

        if (!rule.isEffectiveOn(asOf)) {
            throw new BadRequestException("Rule is not active for the given date",
                Map.of("ruleCode", ruleCode, "asOf", asOf.toString()));
        }
        return rule;
    }
}
