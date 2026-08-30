package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.Matter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record MatterCreateRequest(@NotBlank String title,
                                  @NotNull Matter.Type type,
                                  String docketNumber,
                                  Set<UUID> watcherUserIds) {
}
