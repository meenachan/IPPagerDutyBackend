package com.IpPagerDuty.ipDeadlineTracker.job;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.DeadlineEscalationPolicyRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.DeadlineRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.NotificationRepository;
import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class ScheduledJob {
    private static final String DEFAULT_OWNER_RECIPIENT = "__DEFAULT_OWNER__";
    private static final Logger logger = LoggerFactory.getLogger(ScheduledJob.class);
    private final NotificationRepository notificationRepository;
    private final DeadlineRepository deadlineRepository;
    private final NotificationSender notificationSender;
    private final AuditService auditService;
    private final DeadlineEscalationPolicyRepository depRepository;
    private final AppProperties appProperties;

    public ScheduledJob(NotificationRepository notificationRepository,
                        DeadlineRepository deadlineRepository,
                        NotificationSender notificationSender,
                        AuditService auditService,
                        DeadlineEscalationPolicyRepository depRepository,
                        AppProperties appProperties) {
        this.notificationRepository = notificationRepository;
        this.deadlineRepository = deadlineRepository;
        this.notificationSender = notificationSender;
        this.auditService = auditService;
        this.depRepository = depRepository;
        this.appProperties = appProperties;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void processNotificationsAndDeadlines() {
        Instant now = Instant.now();
        markMissedDeadlines(now);
        ensureDefaultEscalations(now);
        sendPendingNotifications(now);
    }

    private void ensureDefaultEscalations(Instant now) {
        for (Deadline deadline : deadlineRepository.findByStatus(Deadline.Status.OPEN)) {
            if (!depRepository.findByDeadlineId(deadline.getId()).isEmpty()
                || notificationRepository.existsActiveDefaultEscalation(deadline.getId(), DEFAULT_OWNER_RECIPIENT)) {
                continue;
            }
            String timezone = deadline.getMatter().getOrganization().getTimezone();
            ZoneId zone = ZoneId.of(timezone == null || timezone.isBlank() ? "UTC" : timezone);
            LocalDate today = LocalDate.ofInstant(now, zone);
            if (!deadline.getDueDate().isAfter(today)) continue;
            Instant scheduled = deadline.getDueDate().atStartOfDay(zone).minusDays(7).toInstant();
            Notification notification = new Notification();
            notification.setDeadline(deadline);
            notification.setType(Notification.Type.ESCALATION);
            notification.setRecipientEmail(DEFAULT_OWNER_RECIPIENT);
            notification.setScheduledFor(scheduled.isAfter(now) ? scheduled : now);
            notification.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
            notificationRepository.save(notification);
        }
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
            ZoneId timezone = workspaceZone(deadline);
            String subject = buildSubject(n, deadline, matter, timezone);
            String textBody = buildTextBody(n, deadline, matter, timezone);
            String htmlBody = buildHtmlBody(n, deadline, matter, timezone);

            if (n.getType() == Notification.Type.REMINDER) {
                String email = deadline.getResponsibleUser().getEmail();
                notificationSender.sendHtml(email, subject, textBody, htmlBody);
                auditService.record(matter.getOrganization(), null, "DEADLINE", deadline.getId(), "reminder_sent", Map.of(
                    "notificationId", n.getId().toString()
                ));
            } else {
                sendEscalations(n, deadline);
                auditService.record(matter.getOrganization(), null, "DEADLINE", deadline.getId(), "escalation_sent", Map.of(
                    "notificationId", n.getId().toString()
                ));
            }
        }
    }

    private void sendEscalations(Notification notification, Deadline deadline) {
        List<DeadlineEscalationPolicy> deps = depRepository.findByDeadlineId(deadline.getId());
        if (notification.getRecipientEmail() != null && notification.getRecipientEmail().equals(DEFAULT_OWNER_RECIPIENT)) {
            notificationSender.sendHtml(deadline.getResponsibleUser().getEmail(),
                "Action needed: " + deadline.getType() + " deadline is within 7 days",
                buildTextBody(notification, deadline, deadline.getMatter(), workspaceZone(deadline)),
                buildHtmlBody(notification, deadline, deadline.getMatter(), workspaceZone(deadline)));
            return;
        }
        for (DeadlineEscalationPolicy dep : deps) {
            EscalationPolicy policy = dep.getEscalationPolicy();
            EscalationEmailGroup group = policy.getEmailGroup();
            for (String email : group.getEmails()) {
                notificationSender.sendHtml(email,
                    "Escalation: " + deadline.getType() + " deadline",
                    buildTextBody(notification, deadline, deadline.getMatter(), workspaceZone(deadline)),
                    buildHtmlBody(notification, deadline, deadline.getMatter(), workspaceZone(deadline)));
            }
        }
    }

    private String buildSubject(Notification n, Deadline deadline, Matter matter, ZoneId timezone) {
        long days = daysUntil(deadline, timezone);
        return n.getType() == Notification.Type.REMINDER
            ? "Reminder: " + deadline.getType() + " is due in " + days + " day" + (days == 1 ? "" : "s")
            : "Action needed: " + deadline.getType() + " is due in " + days + " day" + (days == 1 ? "" : "s");
    }

    private String buildTextBody(Notification n, Deadline deadline, Matter matter, ZoneId timezone) {
        long days = daysUntil(deadline, timezone);
        String kind = n.getType() == Notification.Type.REMINDER ? "Reminder" : "Escalation";
        return kind + "\n\n"
            + "Case: " + matter.getTitle() + "\n"
            + "Deadline: " + deadline.getType() + "\n"
            + "Due: " + deadline.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\n"
            + "Time remaining: " + days + " day" + (days == 1 ? "" : "s") + "\n"
            + "Status: " + deadline.getStatus() + "\n\n"
            + (n.getType() == Notification.Type.REMINDER
                ? "This is a scheduled reminder for the responsible person."
                : "This deadline is still open and is within the final 7 days.")
            + "\n\nView deadline: " + deadlineUrl(deadline);
    }

    private String buildHtmlBody(Notification n, Deadline deadline, Matter matter, ZoneId timezone) {
        boolean escalation = n.getType() == Notification.Type.ESCALATION;
        long days = daysUntil(deadline, timezone);
        String accent = escalation ? "#b7791f" : "#635bdb";
        String background = escalation ? "#fff5df" : "#eeedff";
        String title = escalation ? "Action needed" : "Upcoming deadline reminder";
        String explanation = escalation
            ? "This deadline is still open and is within the final 7 days."
            : "This is a scheduled reminder for the responsible person.";
        return """
            <div style="margin:0;background:#f7f7f8;padding:32px 16px;font-family:Arial,sans-serif;color:#17171a">
              <div style="max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #e7e7ec;border-radius:16px;overflow:hidden">
                <div style="padding:22px 24px;background:%s;color:%s;font-weight:700;letter-spacing:.04em">%s</div>
                <div style="padding:28px 24px">
                  <div style="font-size:22px;font-weight:700;margin-bottom:8px">%s</div>
                  <p style="margin:0 0 22px;color:#686872;line-height:1.5">%s</p>
                  <div style="border:1px solid #e7e7ec;border-radius:12px;padding:16px;margin-bottom:22px">
                    <div style="font-size:12px;color:#9696a2;text-transform:uppercase;letter-spacing:.08em">Case</div>
                    <div style="font-size:16px;font-weight:700;margin:5px 0 16px">%s</div>
                    <div style="font-size:12px;color:#9696a2;text-transform:uppercase;letter-spacing:.08em">Deadline</div>
                    <div style="font-size:16px;font-weight:700;margin:5px 0 16px">%s</div>
                    <div style="font-size:12px;color:#9696a2;text-transform:uppercase;letter-spacing:.08em">Due date</div>
                    <div style="font-size:16px;font-weight:700;margin:5px 0">%s</div>
                    <div style="color:%s;font-weight:700;margin-top:12px">%s day%s remaining</div>
                  </div>
                  <a href="%s" style="display:inline-block;background:#635bdb;color:#ffffff;text-decoration:none;border-radius:999px;padding:12px 18px;font-weight:700">View deadline</a>
                </div>
                <div style="padding:16px 24px;border-top:1px solid #e7e7ec;color:#9696a2;font-size:12px;line-height:1.5">IPPagerDuty · Workspace timezone: %s</div>
              </div>
            </div>
            """.formatted(
                background, accent, escalation ? "ESCALATION" : "REMINDER", title, explanation,
                escapeHtml(matter.getTitle()), escapeHtml(deadline.getType()),
                deadline.getDueDate(), accent, days, days == 1 ? "" : "s",
                deadlineUrl(deadline), escapeHtml(timezone.getId()));
    }

    private long daysUntil(Deadline deadline, ZoneId timezone) {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(timezone), deadline.getDueDate());
    }

    private ZoneId workspaceZone(Deadline deadline) {
        String timezone = deadline.getMatter().getOrganization().getTimezone();
        return ZoneId.of(timezone == null || timezone.isBlank() ? "UTC" : timezone);
    }

    private String deadlineUrl(Deadline deadline) {
        return appProperties.getFrontend().getBaseUrl() + "/deadline/" + deadline.getId();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
