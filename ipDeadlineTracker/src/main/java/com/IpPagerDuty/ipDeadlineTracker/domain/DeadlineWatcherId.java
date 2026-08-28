package com.IpPagerDuty.ipDeadlineTracker.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class DeadlineWatcherId implements Serializable {
    private UUID deadline;
    private UUID user;

    public DeadlineWatcherId() {}

    public DeadlineWatcherId(UUID deadline, UUID user) {
        this.deadline = deadline;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeadlineWatcherId that)) return false;
        return Objects.equals(deadline, that.deadline) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deadline, user);
    }
}
