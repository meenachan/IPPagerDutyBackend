package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.User;

import java.util.UUID;

/** Display-ready user information so the frontend never has to resolve raw UUIDs on its own. */
public record UserSummaryResponse(UUID id, String email, String displayName) {
    public static UserSummaryResponse from(User user) {
        if (user == null) return null;
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.resolveDisplayName());
    }
}
