package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record DeadlineCreateRequest(@NotNull LocalDate dueDate,
                                    @NotBlank String type,
                                    UUID responsibleUserId,
                                    Set<UUID> watcherUserIds) {
}
