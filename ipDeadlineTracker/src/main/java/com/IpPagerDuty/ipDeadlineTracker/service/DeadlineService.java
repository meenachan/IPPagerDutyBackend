package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import com.IpPagerDuty.ipDeadlineTracker.security.ForbiddenException;
import com.IpPagerDuty.ipDeadlineTracker.security.NotFoundException;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineCreateRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeadlineService {
    private final DeadlineRepository deadlineRepository;
    private final MatterRepository matterRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DeadlineWatcherRepository watcherRepository;
    private final NotificationRepository notificationRepository;
    private final DeadlineEscalationPolicyRepository escalationPolicyRepository;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public DeadlineService(DeadlineRepository deadlineRepository,
                           MatterRepository matterRepository,
                           OrganizationMemberRepository memberRepository,
                           UserRepository userRepository,
                           DeadlineWatcherRepository watcherRepository,
                           NotificationRepository notificationRepository,
                           DeadlineEscalationPolicyRepository escalationPolicyRepository,
                           AuditService auditService,
                           AppProperties appProperties) {
        this.deadlineRepository = deadlineRepository;
        this.matterRepository = matterRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.watcherRepository = watcherRepository;
        this.notificationRepository = notificationRepository;
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    @Transactional
    public Deadline create(UUID matterId, DeadlineCreateRequest request, User user) {
        Matter matter = matterRepository.findById(matterId).orElseThrow(() -> new NotFoundException("matter not found"));
        OrganizationMember member = ensureMemberOf(matter.getOrganization().getId(), user);
        if (member.getRole() == OrganizationMember.Role.CLIENT && !matter.getOwner().getId().equals(user.getId())) {
            throw new NotFoundException("matter not found");
        }

        User responsible = user;
        if (request.responsibleUserId() != null) {
            if (member.getRole() == OrganizationMember.Role.CLIENT) {
                throw new ForbiddenException("clients cannot assign responsible person");
            }
            responsible = userRepository.findById(request.responsibleUserId())
                .orElseThrow(() -> new NotFoundException("responsible user not found"));
        }

        Deadline deadline = new Deadline();
        deadline.setMatter(matter);
        deadline.setDueDate(request.dueDate());
        deadline.setType(request.type());
        deadline.setResponsibleUser(responsible);
        deadline.setCreatedBy(user);
        deadlineRepository.save(deadline);

        addWatchers(deadline, request.watcherUserIds(), matter.getOrganization().getId());
        materializeReminders(deadline);

        auditService.record(matter.getOrganization(), user, "DEADLINE", deadline.getId(), "created", Map.of(
            "dueDate", deadline.getDueDate().toString(),
            "type", deadline.getType(),
            "responsibleUserId", responsible.getId().toString()
        ));
        return deadline;
    }

    public List<Deadline> list(UUID matterId, User user) {
        Matter matter = matterRepository.findById(matterId).orElseThrow(() -> new NotFoundException("matter not found"));
        ensureMemberOf(matter.getOrganization().getId(), user);
        return deadlineRepository.findByMatterId(matterId);
    }

    public Deadline get(UUID deadlineId, User user) {
        Deadline deadline = deadlineRepository.findByIdWithWatchers(deadlineId)
            .orElseThrow(() -> new NotFoundException("deadline not found"));
        ensureCanView(deadline, user);
        return deadline;
    }

    @Transactional
    public Deadline update(UUID deadlineId, DeadlineUpdateRequest request, User user) {
        Deadline deadline = get(deadlineId, user);
        OrganizationMember member = ensureMemberOf(deadline.getMatter().getOrganization().getId(), user);
        if (request.dueDate() != null) {
            deadline.setDueDate(request.dueDate());
        }
        if (request.responsibleUserId() != null) {
            if (member.getRole() == OrganizationMember.Role.CLIENT) {
                throw new ForbiddenException("clients cannot assign responsible person");
            }
            User responsible = userRepository.findById(request.responsibleUserId())
                .orElseThrow(() -> new NotFoundException("responsible user not found"));
            deadline.setResponsibleUser(responsible);
        }
        if (request.status() != null) {
            deadline.setStatus(request.status());
            if (request.status() == Deadline.Status.COMPLETED) {
                deadline.setCompletedAt(Instant.now());
                cancelFutureNotifications(deadline);
            }
        }
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadline.getId(), "updated", Map.of(
            "dueDate", deadline.getDueDate().toString(),
            "responsibleUserId", deadline.getResponsibleUser().getId().toString(),
            "status", deadline.getStatus().name()
        ));
        return deadline;
    }

    @Transactional
    public Deadline complete(UUID deadlineId, User user) {
        Deadline deadline = get(deadlineId, user);
        OrganizationMember member = ensureMemberOf(deadline.getMatter().getOrganization().getId(), user);
        if (deadline.getStatus() == Deadline.Status.COMPLETED) {
            return deadline;
        }
        if (!canMarkComplete(deadline, member, user)) {
            throw new ForbiddenException("cannot mark this deadline complete");
        }
        deadline.setStatus(Deadline.Status.COMPLETED);
        deadline.setCompletedAt(Instant.now());
        cancelFutureNotifications(deadline);
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadline.getId(), "completed", Map.of());
        return deadline;
    }

    private boolean canMarkComplete(Deadline deadline, OrganizationMember member, User user) {
        return switch (member.getRole()) {
            case BUSINESS_OWNER -> deadline.getMatter().getOwner().getId().equals(user.getId())
                || deadline.getResponsibleUser().getId().equals(user.getId());
            case LAWYER, PARALEGAL -> deadline.getResponsibleUser().getId().equals(user.getId());
            default -> false;
        };
    }

    @Transactional
    public void archive(UUID deadlineId, User user) {
        Deadline deadline = get(deadlineId, user);
        ensureMemberOf(deadline.getMatter().getOrganization().getId(), user);
        if (deadline.getStatus() == Deadline.Status.ARCHIVED) {
            return;
        }
        deadline.setStatus(Deadline.Status.ARCHIVED);
        cancelFutureNotifications(deadline);
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadline.getId(), "archived", Map.of());
    }

    @Transactional
    public Deadline markNotDone(UUID deadlineId, Deadline.NotDoneReason reason, User user) {
        Deadline deadline = get(deadlineId, user);
        ensureMemberOf(deadline.getMatter().getOrganization().getId(), user);
        deadline.setNotDoneReason(reason);
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadline.getId(), "not_done_reason_set", Map.of(
            "reason", reason.name()
        ));
        return deadline;
    }

    public List<EscalationPolicy> attachedPolicies(UUID deadlineId) {
        return escalationPolicyRepository.findByDeadlineId(deadlineId).stream()
            .map(DeadlineEscalationPolicy::getEscalationPolicy)
            .toList();
    }

    private void cancelFutureNotifications(Deadline deadline) {
        notificationRepository.cancelPendingByDeadline(deadline.getId());
    }

    public void materializeReminders(Deadline deadline) {
        Organization org = deadline.getMatter().getOrganization();
        ZoneId timezone = ZoneId.of(org.getTimezone() == null || org.getTimezone().isBlank() ? "UTC" : org.getTimezone());
        List<Integer> offsets = parseOffsets(org.getReminderOffsetsDays());
        for (int offset : offsets) {
            Instant scheduled = deadline.getDueDate().atStartOfDay(timezone).minusDays(offset).toInstant();
            if (!scheduled.isAfter(Instant.now())) continue;
            Notification n = new Notification();
            n.setDeadline(deadline);
            n.setType(Notification.Type.REMINDER);
            n.setScheduledFor(scheduled);
            n.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
            notificationRepository.save(n);
        }
        materializeDefaultEscalation(deadline, timezone);
    }

    private void materializeDefaultEscalation(Deadline deadline, ZoneId timezone) {
        LocalDate today = LocalDate.now(timezone);
        if (!deadline.getDueDate().isAfter(today)) return;
        Instant scheduled = deadline.getDueDate().atStartOfDay(timezone).minusDays(7).toInstant();
        Notification n = new Notification();
        n.setDeadline(deadline);
        n.setType(Notification.Type.ESCALATION);
        n.setRecipientEmail("__DEFAULT_OWNER__");
        n.setScheduledFor(scheduled.isAfter(Instant.now()) ? scheduled : Instant.now());
        n.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
        notificationRepository.save(n);
    }

    @Transactional
    public void rescheduleRemindersForOrganization(UUID organizationId) {
        deadlineRepository.findByStatus(Deadline.Status.OPEN).stream()
            .filter(deadline -> deadline.getMatter().getOrganization().getId().equals(organizationId))
            .forEach(deadline -> {
                notificationRepository.deletePendingByDeadline(deadline.getId());
                materializeReminders(deadline);
            });
    }

    private List<Integer> parseOffsets(String raw) {
        if (raw == null || raw.isBlank()) {
            return appProperties.getReminders().getOffsetsDays();
        }
        return java.util.Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Integer::parseInt)
            .toList();
    }

    private void addWatchers(Deadline deadline, Set<UUID> watcherIds, UUID orgId) {
        if (watcherIds == null) return;
        Set<UUID> orgUserIds = memberRepository.findByOrganizationId(orgId).stream()
            .map(m -> m.getUser().getId())
            .collect(Collectors.toSet());
        for (UUID uid : watcherIds) {
            if (!orgUserIds.contains(uid)) continue;
            DeadlineWatcher w = new DeadlineWatcher();
            w.setDeadline(deadline);
            w.setUser(userRepository.getReferenceById(uid));
            deadline.getWatchers().add(w);
        }
    }

    private OrganizationMember ensureMemberOf(UUID orgId, User user) {
        OrganizationMember member = memberRepository.findByUserId(user.getId())
            .orElseThrow(() -> new NotFoundException("organization not found"));
        if (!member.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("organization not found");
        }
        return member;
    }

    private void ensureCanView(Deadline deadline, User user) {
        Matter matter = deadline.getMatter();
        OrganizationMember member = ensureMemberOf(matter.getOrganization().getId(), user);
        boolean ok = switch (member.getRole()) {
            case CLIENT -> matter.getOwner().getId().equals(user.getId());
            case PARALEGAL -> true;
            default -> matter.getOwner().getId().equals(user.getId())
                || isWatcher(matter, user)
                || deadline.getResponsibleUser().getId().equals(user.getId());
        };
        if (!ok) throw new NotFoundException("deadline not found");
    }

    private boolean isWatcher(Matter matter, User user) {
        return matter.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(user.getId()));
    }
}
