package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;

import java.time.LocalDate;
import java.util.Map;

/** Small builder for constructing DeadlineRule instances in unit tests without a database. */
final class TestRules {
    private TestRules() {}

    static DeadlineRule addMonths(String code, String officeCode, DeadlineRule.TriggerType triggerType,
                                   int months, Map<String, Object> extraParams) {
        DeadlineRule rule = new DeadlineRule();
        rule.setCode(code);
        rule.setOfficeCode(officeCode);
        rule.setVersion("2026.08");
        rule.setTriggerType(triggerType);
        rule.setCalculationType(DeadlineRule.CalculationType.ADD_MONTHS);
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("months", months);
        if (extraParams != null) params.putAll(extraParams);
        rule.setParameters(params);
        rule.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        rule.setSourceAuthority("TEST");
        rule.setSourceReference("test-reference");
        rule.setActive(true);
        return rule;
    }

    static DeadlineRule laterOf(String code, int priorityMonths, int isrMonths) {
        DeadlineRule rule = new DeadlineRule();
        rule.setCode(code);
        rule.setVersion("2026.08");
        rule.setTriggerType(DeadlineRule.TriggerType.EARLIEST_PRIORITY);
        rule.setCalculationType(DeadlineRule.CalculationType.LATER_OF);
        rule.setParameters(Map.of("priorityMonths", priorityMonths, "isrMonths", isrMonths));
        rule.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        rule.setSourceAuthority("TEST");
        rule.setSourceReference("test-reference");
        rule.setActive(true);
        return rule;
    }
}
