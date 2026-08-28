package com.IpPagerDuty.ipDeadlineTracker.security;

import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;

public record AuthContext(User user, OrganizationMember membership) {
}
