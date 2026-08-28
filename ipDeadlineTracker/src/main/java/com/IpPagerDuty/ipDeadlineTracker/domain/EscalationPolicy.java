package com.IpPagerDuty.ipDeadlineTracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "escalation_policies")
public class EscalationPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Column(name = "trigger_offset_days", nullable = false)
    private int triggerOffsetDays;

    @ManyToOne(optional = false)
    @JoinColumn(name = "email_group_id", nullable = false)
    private EscalationEmailGroup emailGroup;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum TriggerType { BEFORE_DUE, AFTER_DUE }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public int getTriggerOffsetDays() { return triggerOffsetDays; }
    public void setTriggerOffsetDays(int triggerOffsetDays) { this.triggerOffsetDays = triggerOffsetDays; }

    public EscalationEmailGroup getEmailGroup() { return emailGroup; }
    public void setEmailGroup(EscalationEmailGroup emailGroup) { this.emailGroup = emailGroup; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
