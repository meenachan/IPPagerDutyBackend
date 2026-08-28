package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(@NotBlank @Email String email,
                                  @NotNull OrganizationMember.Role role) {
}
