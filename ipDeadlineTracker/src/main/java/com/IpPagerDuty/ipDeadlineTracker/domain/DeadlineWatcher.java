package com.IpPagerDuty.ipDeadlineTracker.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "deadline_watchers")
@IdClass(DeadlineWatcherId.class)
public class DeadlineWatcher {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "deadline_id", nullable = false)
    private Deadline deadline;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Deadline getDeadline() { return deadline; }
    public void setDeadline(Deadline deadline) { this.deadline = deadline; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
