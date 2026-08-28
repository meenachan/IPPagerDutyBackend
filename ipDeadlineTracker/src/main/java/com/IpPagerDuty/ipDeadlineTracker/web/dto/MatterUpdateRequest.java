package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import java.util.Set;
import java.util.UUID;

public record MatterUpdateRequest(String title, Set<UUID> watcherUserIds) {
}
