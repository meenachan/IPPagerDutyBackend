package com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CalculationResponse(
    UUID calculationId,
    String ruleCode,
    String ruleVersion,
    String officeCode,
    LocalDate triggerDate,
    LocalDate calculatedDate,
    boolean estimated,
    List<String> trace,
    List<String> warnings,
    Source source
) {
    public record Source(String authority, String reference, String url) {}
}
