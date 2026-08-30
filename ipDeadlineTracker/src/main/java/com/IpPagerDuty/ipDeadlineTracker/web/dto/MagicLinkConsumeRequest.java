package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MagicLinkConsumeRequest(@NotBlank String token) {
}
