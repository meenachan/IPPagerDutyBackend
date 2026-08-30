package com.IpPagerDuty.ipDeadlineTracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "deadlines")
public class Deadline {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "matter_id", nullable = false)
    private Matter matter;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private User responsibleUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "not_done_reason")
    private NotDoneReason notDoneReason;

    /** Free-text notes, e.g. imported from the CSV "notes" column. */
    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "deadline", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<DeadlineWatcher> watchers = new HashSet<>();

    public enum Status { OPEN, COMPLETED, MISSED, ARCHIVED }
    public enum NotDoneReason { WAITING_ON_CLIENT, WAITING_ON_OFFICE, NEED_MORE_TIME, INTERNAL_REVIEW, OTHER }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Matter getMatter() { return matter; }
    public void setMatter(Matter matter) { this.matter = matter; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public User getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(User responsibleUser) { this.responsibleUser = responsibleUser; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public NotDoneReason getNotDoneReason() { return notDoneReason; }
    public void setNotDoneReason(NotDoneReason notDoneReason) { this.notDoneReason = notDoneReason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Set<DeadlineWatcher> getWatchers() { return watchers; }
    public void setWatchers(Set<DeadlineWatcher> watchers) { this.watchers = watchers; }
}
