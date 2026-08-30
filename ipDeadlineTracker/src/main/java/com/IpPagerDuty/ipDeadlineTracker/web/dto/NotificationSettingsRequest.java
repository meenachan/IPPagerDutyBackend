package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import java.util.List;

public record NotificationSettingsRequest(List<Integer> reminderOffsetsDays) {
}
