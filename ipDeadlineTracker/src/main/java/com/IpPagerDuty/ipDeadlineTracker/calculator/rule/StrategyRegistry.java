package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Dispatches to the CalculationStrategy that matches a rule's
 * calculationType. New calculation "shapes" are added as strategies;
 * new offices/rules that reuse an existing shape are pure configuration.
 */
@Component
public class StrategyRegistry {
    private final List<CalculationStrategy> strategies;

    public StrategyRegistry(List<CalculationStrategy> strategies) {
        this.strategies = strategies;
    }

    public CalculationStrategy resolve(DeadlineRule.CalculationType calculationType) {
        return strategies.stream()
            .filter(s -> s.supports() == calculationType)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("No calculation strategy is available for this rule",
                Map.of("calculationType", calculationType.name())));
    }
}
