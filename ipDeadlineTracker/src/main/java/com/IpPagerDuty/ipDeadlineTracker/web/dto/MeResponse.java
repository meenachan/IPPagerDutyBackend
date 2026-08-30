package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;

import java.util.List;
import java.util.UUID;

public record MeResponse(UserSummaryResponse user, List<OrganizationSummary> organizations, UUID activeOrganizationId) {
    public record OrganizationSummary(UUID id, String name, String role) {}

    /** Currently a user belongs to at most one organization, so the list always has 0 or 1 entries. */
    public static MeResponse from(User user, OrganizationMember membership) {
        List<OrganizationSummary> orgs = membership == null
            ? List.of()
            : List.of(new OrganizationSummary(membership.getOrganization().getId(), membership.getOrganization().getName(), membership.getRole().name()));
        UUID activeOrgId = membership == null ? null : membership.getOrganization().getId();
        return new MeResponse(UserSummaryResponse.from(user), orgs, activeOrgId);
    }
}
