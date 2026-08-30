package com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto;

import java.util.List;

public record RuleSummaryResponse(
    String code,
    String officeCode,
    String jurisdiction,
    String version,
    List<String> requiredInputs,
    String sourceAuthority,
    String sourceReference
) {}
