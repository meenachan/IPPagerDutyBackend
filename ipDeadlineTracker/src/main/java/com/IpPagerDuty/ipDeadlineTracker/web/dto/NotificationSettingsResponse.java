package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import java.util.List;

public record NotificationSettingsResponse(List<Integer> reminderOffsetsDays, String timezone) {
}
