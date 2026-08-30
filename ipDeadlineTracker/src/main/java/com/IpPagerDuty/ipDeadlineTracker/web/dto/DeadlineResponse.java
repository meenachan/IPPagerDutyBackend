package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import com.IpPagerDuty.ipDeadlineTracker.domain.DeadlineWatcher;
import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationPolicy;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
                               String notDoneReason,
                               String notes,
                               Set<UUID> watcherUserIds,
                               UserSummaryResponse responsibleUser,
                               MatterSummary matter,
                               List<EscalationPolicySummary> escalationPolicies) {

    public record MatterSummary(UUID id, String title) {}
    public record EscalationPolicySummary(UUID id, String name, String triggerType, int triggerOffsetDays) {
        public static EscalationPolicySummary from(EscalationPolicy p) {
            return new EscalationPolicySummary(p.getId(), p.getName(), p.getTriggerType().name(), p.getTriggerOffsetDays());
        }
    }

    public static DeadlineResponse from(Deadline d) {
        return from(d, List.of());
    }

    public static DeadlineResponse from(Deadline d, List<EscalationPolicy> policies) {
        return new DeadlineResponse(
            d.getId(),
            d.getMatter().getId(),
            d.getDueDate(),
            d.getType(),
            d.getResponsibleUser().getId(),
            d.getStatus().name(),
            d.getCompletedAt(),
            d.getNotDoneReason() == null ? null : d.getNotDoneReason().name(),
            d.getNotes(),
            d.getWatchers().stream().map(DeadlineWatcher::getUser).map(User::getId).collect(Collectors.toSet()),
            UserSummaryResponse.from(d.getResponsibleUser()),
            new MatterSummary(d.getMatter().getId(), d.getMatter().getTitle()),
            policies.stream().map(EscalationPolicySummary::from).toList()
        );
    }
}
