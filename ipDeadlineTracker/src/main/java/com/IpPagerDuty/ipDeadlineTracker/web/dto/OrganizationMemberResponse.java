package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;

import java.util.UUID;

public record OrganizationMemberResponse(UUID id, String email, String displayName, String role) {
    public static OrganizationMemberResponse from(OrganizationMember m) {
        return new OrganizationMemberResponse(m.getId(), m.getUser().getEmail(), m.getUser().resolveDisplayName(), m.getRole().name());
    }
}
