package com.IpPagerDuty.ipDeadlineTracker.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class DeadlineEscalationPolicyId implements Serializable {
    private UUID deadline;
    private UUID escalationPolicy;

    public DeadlineEscalationPolicyId() {}

    public DeadlineEscalationPolicyId(UUID deadline, UUID escalationPolicy) {
        this.deadline = deadline;
        this.escalationPolicy = escalationPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeadlineEscalationPolicyId that)) return false;
        return Objects.equals(deadline, that.deadline) && Objects.equals(escalationPolicy, that.escalationPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deadline, escalationPolicy);
    }
}
