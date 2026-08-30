package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import jakarta.validation.constraints.NotBlank;
import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;

public record OrganizationCreateRequest(@NotBlank String name, OrganizationMember.Role role) {
    public OrganizationCreateRequest(String name) {
        this(name, OrganizationMember.Role.BUSINESS_OWNER);
    }
}
