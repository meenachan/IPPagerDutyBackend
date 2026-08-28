package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import com.IpPagerDuty.ipDeadlineTracker.domain.DeadlineWatcher;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record DeadlineResponse(UUID id,
                               UUID matterId,
                               LocalDate dueDate,
                               String type,
                               UUID responsibleUserId,
                               String status,
                               Instant completedAt,
                               Set<UUID> watcherUserIds) {
    public static DeadlineResponse from(Deadline d) {
        return new DeadlineResponse(
            d.getId(),
            d.getMatter().getId(),
            d.getDueDate(),
            d.getType(),
            d.getResponsibleUser().getId(),
            d.getStatus().name(),
            d.getCompletedAt(),
            d.getWatchers().stream().map(DeadlineWatcher::getUser).map(User::getId).collect(Collectors.toSet())
        );
    }
}
