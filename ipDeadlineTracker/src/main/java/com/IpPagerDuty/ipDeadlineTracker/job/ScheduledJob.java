package com.IpPagerDuty.ipDeadlineTracker.job;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.DeadlineEscalationPolicyRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.DeadlineRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.NotificationRepository;
import com.IpPagerDuty.ipDeadlineTracker.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class ScheduledJob {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledJob.class);
    private final NotificationRepository notificationRepository;
    private final DeadlineRepository deadlineRepository;
    private final NotificationSender notificationSender;
    private final AuditService auditService;
    private final DeadlineEscalationPolicyRepository depRepository;

    public ScheduledJob(NotificationRepository notificationRepository,
                        DeadlineRepository deadlineRepository,
                        NotificationSender notificationSender,
                        AuditService auditService,
                        DeadlineEscalationPolicyRepository depRepository) {
        this.notificationRepository = notificationRepository;
        this.deadlineRepository = deadlineRepository;
        this.notificationSender = notificationSender;
        this.auditService = auditService;
        this.depRepository = depRepository;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void processNotificationsAndDeadlines() {
        Instant now = Instant.now();
        markMissedDeadlines(now);
        sendPendingNotifications(now);
    }

    private void markMissedDeadlines(Instant now) {
        List<Deadline> missed = deadlineRepository.findByStatus(Deadline.Status.OPEN).stream()
            .filter(deadline -> {
                String timezone = deadline.getMatter().getOrganization().getTimezone();
                ZoneId zone = ZoneId.of(timezone == null || timezone.isBlank() ? "UTC" : timezone);
                return deadline.getDueDate().isBefore(LocalDate.ofInstant(now, zone));
            })
            .toList();
        for (Deadline d : missed) {
            d.setStatus(Deadline.Status.MISSED);
            auditService.record(d.getMatter().getOrganization(), null, "DEADLINE", d.getId(), "marked_missed", Map.of(
                "dueDate", d.getDueDate().toString()
            ));
        }
    }

    private void sendPendingNotifications(Instant now) {
        List<Notification> pending = notificationRepository.findPendingDue(now);
        for (Notification n : pending) {
            int claimed = notificationRepository.claimAndMarkSent(n.getId(), now);
            if (claimed == 0) continue;

            Deadline deadline = n.getDeadline();
            Matter matter = deadline.getMatter();
            String subject = buildSubject(n, deadline, matter);
            String body = buildBody(n, deadline, matter);

            if (n.getType() == Notification.Type.REMINDER) {
                String email = deadline.getResponsibleUser().getEmail();
                notificationSender.send(email, subject, body);
                auditService.record(matter.getOrganization(), null, "DEADLINE", deadline.getId(), "reminder_sent", Map.of(
                    "notificationId", n.getId().toString()
                ));
            } else {
                sendEscalations(deadline);
                auditService.record(matter.getOrganization(), null, "DEADLINE", deadline.getId(), "escalation_sent", Map.of(
                    "notificationId", n.getId().toString()
                ));
            }
        }
    }

    private void sendEscalations(Deadline deadline) {
        List<DeadlineEscalationPolicy> deps = depRepository.findByDeadlineId(deadline.getId());
        for (DeadlineEscalationPolicy dep : deps) {
            EscalationPolicy policy = dep.getEscalationPolicy();
            EscalationEmailGroup group = policy.getEmailGroup();
            for (String email : group.getEmails()) {
                notificationSender.send(email,
                    "Escalation: " + deadline.getType() + " deadline",
                    "Deadline for matter " + deadline.getMatter().getTitle() + " is due " + deadline.getDueDate());
            }
        }
    }

    private String buildSubject(Notification n, Deadline deadline, Matter matter) {
        return (n.getType() == Notification.Type.REMINDER ? "Reminder" : "Escalation") +
            ": " + deadline.getType() + " deadline for " + matter.getTitle();
    }

    private String buildBody(Notification n, Deadline deadline, Matter matter) {
        return "Deadline: " + deadline.getType() + "\nMatter: " + matter.getTitle() +
            "\nDue: " + deadline.getDueDate() + "\nStatus: " + deadline.getStatus();
    }
}
