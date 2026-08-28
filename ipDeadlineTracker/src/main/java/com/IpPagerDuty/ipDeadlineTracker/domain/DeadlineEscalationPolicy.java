package com.IpPagerDuty.ipDeadlineTracker.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "deadline_escalation_policies")
@IdClass(DeadlineEscalationPolicyId.class)
public class DeadlineEscalationPolicy {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "deadline_id", nullable = false)
    private Deadline deadline;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "escalation_policy_id", nullable = false)
    private EscalationPolicy escalationPolicy;

    public Deadline getDeadline() { return deadline; }
    public void setDeadline(Deadline deadline) { this.deadline = deadline; }

    public EscalationPolicy getEscalationPolicy() { return escalationPolicy; }
    public void setEscalationPolicy(EscalationPolicy escalationPolicy) { this.escalationPolicy = escalationPolicy; }
}
