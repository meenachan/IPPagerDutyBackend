package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;

/**
 * A calculation strategy implements one calculation "shape" (add months,
 * later-of, etc). Most offices reuse the same strategy purely through
 * configuration (spec section 11) — new strategy classes are only added for
 * rules with genuinely different trigger/combination semantics.
 */
public interface CalculationStrategy {
    DeadlineRule.CalculationType supports();

    CalculationResult calculate(DeadlineRule rule, CalculationContext context);
}
