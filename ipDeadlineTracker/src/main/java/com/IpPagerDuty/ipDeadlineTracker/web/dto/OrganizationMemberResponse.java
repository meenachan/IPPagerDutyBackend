package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;

import java.util.UUID;

public record OrganizationMemberResponse(UUID id, String email, String role) {
    public static OrganizationMemberResponse from(OrganizationMember m) {
        return new OrganizationMemberResponse(m.getId(), m.getUser().getEmail(), m.getRole().name());
    }
}
