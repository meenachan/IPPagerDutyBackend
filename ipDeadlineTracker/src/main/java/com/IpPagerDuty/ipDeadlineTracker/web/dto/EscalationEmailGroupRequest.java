package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record EscalationEmailGroupRequest(@NotBlank String name, List<String> emails) {
}
