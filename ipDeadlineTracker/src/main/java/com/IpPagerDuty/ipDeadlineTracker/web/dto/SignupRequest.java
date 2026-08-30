package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;

public record SignupRequest(@NotBlank @Email String email,
                            @NotBlank String organizationName,
                            String displayName,
                            OrganizationMember.Role role) {
    public SignupRequest(String email, String organizationName, String displayName) {
        this(email, organizationName, displayName, OrganizationMember.Role.BUSINESS_OWNER);
    }
}
