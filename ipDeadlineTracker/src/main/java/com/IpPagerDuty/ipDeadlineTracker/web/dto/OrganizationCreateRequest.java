package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationCreateRequest(@NotBlank String name) {
}
