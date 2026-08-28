package com.IpPagerDuty.ipDeadlineTracker.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class MatterParticipantId implements Serializable {
    private UUID matter;
    private UUID user;

    public MatterParticipantId() {}

    public MatterParticipantId(UUID matter, UUID user) {
        this.matter = matter;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatterParticipantId that)) return false;
        return Objects.equals(matter, that.matter) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matter, user);
    }
}
