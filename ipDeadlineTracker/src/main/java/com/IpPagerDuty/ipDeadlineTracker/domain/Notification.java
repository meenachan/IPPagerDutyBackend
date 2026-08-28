package com.IpPagerDuty.ipDeadlineTracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"deadline_id", "type", "scheduled_for"})
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deadline_id", nullable = false)
    private Deadline deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "recipient_email")
    private String recipientEmail;

    public enum Type { REMINDER, ESCALATION }
    public enum DeliveryStatus { PENDING, SENT, FAILED, CANCELLED }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Deadline getDeadline() { return deadline; }
    public void setDeadline(Deadline deadline) { this.deadline = deadline; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Instant getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
}
