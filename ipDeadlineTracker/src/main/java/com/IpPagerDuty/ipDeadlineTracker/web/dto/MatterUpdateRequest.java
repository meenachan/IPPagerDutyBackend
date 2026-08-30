package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import java.util.Set;
import java.util.UUID;

public record MatterUpdateRequest(String title, String docketNumber, Set<UUID> watcherUserIds) {
}
