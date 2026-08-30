package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.Matter;
import com.IpPagerDuty.ipDeadlineTracker.domain.MatterParticipant;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record MatterResponse(UUID id,
                             String title,
                             String docketNumber,
                             String type,
                             UUID ownerUserId,
                             UUID createdBy,
                             UserSummaryResponse owner,
                             UserSummaryResponse createdByUser,
                             Set<WatcherDto> watchers) {
    public record WatcherDto(UUID userId, String accessLevel) {}

    public static MatterResponse from(Matter matter) {
        return new MatterResponse(
            matter.getId(),
            matter.getTitle(),
            matter.getDocketNumber(),
            matter.getType().name(),
            matter.getOwner().getId(),
            matter.getCreatedBy().getId(),
            UserSummaryResponse.from(matter.getOwner()),
            UserSummaryResponse.from(matter.getCreatedBy()),
            matter.getParticipants().stream()
                .map(p -> new WatcherDto(p.getUser().getId(), p.getAccessLevel().name()))
                .collect(Collectors.toSet())
        );
    }
}
