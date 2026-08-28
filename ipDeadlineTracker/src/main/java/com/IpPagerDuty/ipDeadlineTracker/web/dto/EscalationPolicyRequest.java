package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EscalationPolicyRequest(@NotBlank String name,
                                      @NotNull EscalationPolicy.TriggerType triggerType,
                                      int triggerOffsetDays,
                                      @NotNull UUID emailGroupId) {
}
