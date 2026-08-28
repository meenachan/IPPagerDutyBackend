package com.IpPagerDuty.ipDeadlineTracker.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "matter_participants")
@IdClass(MatterParticipantId.class)
public class MatterParticipant {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "matter_id", nullable = false)
    private Matter matter;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel;

    public enum AccessLevel { OWNER, WATCHER }

    public Matter getMatter() { return matter; }
    public void setMatter(Matter matter) { this.matter = matter; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }
}
