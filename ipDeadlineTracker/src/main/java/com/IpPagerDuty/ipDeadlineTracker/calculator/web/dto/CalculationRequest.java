package com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto;

import java.util.List;

/**
 * Raw calculation inputs as supplied by the caller. Every date is an ISO
 * YYYY-MM-DD string; parsing/validation happens in CalculationInputValidator
 * so malformed dates always produce a typed validation error rather than an
 * exception leaking from date parsing deep in a strategy.
 */
public record CalculationRequest(
    String ruleCode,
    String officeCode,
    String chapter,
    CalculationInputs inputs
) {
    public record CalculationInputs(
        List<String> priorityDates,
        String earliestPriorityDate,
        String internationalFilingDate,
        String firstFilingDate,
        String isrTransmittalDate,
        String isrWoTransmittalDate,
        Boolean allowEstimate
    ) {}
}
