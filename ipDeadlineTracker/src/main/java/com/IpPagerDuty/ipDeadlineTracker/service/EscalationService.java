package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import com.IpPagerDuty.ipDeadlineTracker.job.NotificationSender;
import com.IpPagerDuty.ipDeadlineTracker.security.ConflictException;
import com.IpPagerDuty.ipDeadlineTracker.security.ForbiddenException;
import com.IpPagerDuty.ipDeadlineTracker.security.NotFoundException;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.EscalationEmailGroupRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.EscalationPolicyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EscalationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final EscalationEmailGroupRepository emailGroupRepository;
    private final EscalationPolicyRepository policyRepository;
    private final DeadlineRepository deadlineRepository;
    private final DeadlineEscalationPolicyRepository depRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final AuditService auditService;

    public EscalationService(OrganizationRepository organizationRepository,
                             OrganizationMemberRepository memberRepository,
                             EscalationEmailGroupRepository emailGroupRepository,
                             EscalationPolicyRepository policyRepository,
                             DeadlineRepository deadlineRepository,
                             DeadlineEscalationPolicyRepository depRepository,
                             NotificationRepository notificationRepository,
                             NotificationSender notificationSender,
                             AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.emailGroupRepository = emailGroupRepository;
        this.policyRepository = policyRepository;
        this.deadlineRepository = deadlineRepository;
        this.depRepository = depRepository;
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
        this.auditService = auditService;
    }

    private OrganizationMember ensureStaff(UUID orgId, User user) {
        OrganizationMember member = memberRepository.findByUserId(user.getId())
            .orElseThrow(() -> new NotFoundException("organization not found"));
        if (!member.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("organization not found");
        }
        if (member.getRole() == OrganizationMember.Role.CLIENT || member.getRole() == OrganizationMember.Role.PARALEGAL) {
            throw new ForbiddenException("insufficient permissions");
        }
        return member;
    }

    @Transactional
    public EscalationEmailGroup createEmailGroup(UUID orgId, EscalationEmailGroupRequest request, User user) {
        ensureStaff(orgId, user);
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NotFoundException("organization not found"));
        EscalationEmailGroup group = new EscalationEmailGroup();
        group.setOrganization(org);
        group.setName(request.name());
        group.setEmails(request.emails() == null ? List.of() : request.emails());
        emailGroupRepository.save(group);
        auditService.record(org, user, "ESCALATION_EMAIL_GROUP", group.getId(), "created", Map.of("name", group.getName()));
        return group;
    }

    public List<EscalationEmailGroup> listEmailGroups(UUID orgId, User user) {
        ensureStaff(orgId, user);
        return emailGroupRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public EscalationPolicy createPolicy(UUID orgId, EscalationPolicyRequest request, User user) {
        ensureStaff(orgId, user);
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NotFoundException("organization not found"));
        EscalationEmailGroup group = emailGroupRepository.findById(request.emailGroupId())
            .orElseThrow(() -> new NotFoundException("email group not found"));
        if (!group.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("email group not found");
        }
        EscalationPolicy policy = new EscalationPolicy();
        policy.setOrganization(org);
        policy.setName(request.name());
        policy.setTriggerType(request.triggerType());
        policy.setTriggerOffsetDays(request.triggerOffsetDays());
        policy.setEmailGroup(group);
        policyRepository.save(policy);
        auditService.record(org, user, "ESCALATION_POLICY", policy.getId(), "created", Map.of("name", policy.getName()));
        return policy;
    }

    public List<EscalationPolicy> listPolicies(UUID orgId, User user) {
        ensureStaff(orgId, user);
        return policyRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public void attachPolicy(UUID deadlineId, UUID policyId, User user) {
        Deadline deadline = deadlineRepository.findById(deadlineId).orElseThrow(() -> new NotFoundException("deadline not found"));
        ensureStaff(deadline.getMatter().getOrganization().getId(), user);
        EscalationPolicy policy = policyRepository.findById(policyId).orElseThrow(() -> new NotFoundException("policy not found"));
        if (!policy.getOrganization().getId().equals(deadline.getMatter().getOrganization().getId())) {
            throw new NotFoundException("policy not found");
        }
        notificationRepository.cancelPendingEscalationsByDeadline(deadlineId);
        DeadlineEscalationPolicy dep = new DeadlineEscalationPolicy();
        dep.setDeadline(deadline);
        dep.setEscalationPolicy(policy);
        depRepository.save(dep);
        materializeEscalation(deadline, policy);
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadlineId, "escalation_policy_attached", Map.of("policyId", policyId.toString()));
    }

    /**
     * Sends a policy's escalation emails immediately (staff-triggered "escalate now"),
     * rather than waiting for the scheduled trigger date. Records a real audit event
     * so the frontend never has to fake this action locally.
     */
    @Transactional
    public void escalateNow(UUID deadlineId, UUID policyId, User user) {
        Deadline deadline = deadlineRepository.findById(deadlineId).orElseThrow(() -> new NotFoundException("deadline not found"));
        ensureStaff(deadline.getMatter().getOrganization().getId(), user);
        DeadlineEscalationPolicy dep = depRepository.findById(new DeadlineEscalationPolicyId(deadlineId, policyId))
            .orElseThrow(() -> new NotFoundException("policy not attached"));
        EscalationPolicy policy = dep.getEscalationPolicy();
        EscalationEmailGroup group = policy.getEmailGroup();
        for (String email : group.getEmails()) {
            notificationSender.send(email,
                "Escalation: " + deadline.getType() + " deadline",
                "Deadline for matter " + deadline.getMatter().getTitle() + " is due " + deadline.getDueDate());
        }
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadlineId, "escalation_sent_manual", Map.of(
            "policyId", policyId.toString(),
            "emailGroupId", group.getId().toString()
        ));
    }

    @Transactional
    public void detachPolicy(UUID deadlineId, UUID policyId, User user) {
        Deadline deadline = deadlineRepository.findById(deadlineId).orElseThrow(() -> new NotFoundException("deadline not found"));
        ensureStaff(deadline.getMatter().getOrganization().getId(), user);
        DeadlineEscalationPolicy dep = depRepository.findById(new DeadlineEscalationPolicyId(deadlineId, policyId))
            .orElseThrow(() -> new NotFoundException("policy not attached"));
        depRepository.delete(dep);
        auditService.record(deadline.getMatter().getOrganization(), user, "DEADLINE", deadlineId, "escalation_policy_detached", Map.of("policyId", policyId.toString()));
    }

    public List<EscalationPolicy> listAttachedPolicies(UUID deadlineId, User user) {
        Deadline deadline = deadlineRepository.findById(deadlineId).orElseThrow(() -> new NotFoundException("deadline not found"));
        ensureStaff(deadline.getMatter().getOrganization().getId(), user);
        return depRepository.findByDeadlineId(deadlineId).stream()
            .map(DeadlineEscalationPolicy::getEscalationPolicy)
            .toList();
    }

    @Transactional
    public void deleteEmailGroup(UUID orgId, UUID groupId, User user) {
        ensureStaff(orgId, user);
        EscalationEmailGroup group = emailGroupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("email group not found"));
        if (!group.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("email group not found");
        }
        List<EscalationPolicy> refs = policyRepository.findByEmailGroupId(groupId);
        if (!refs.isEmpty()) {
            throw new ConflictException("email group is referenced by policies");
        }
        emailGroupRepository.delete(group);
    }

    private void materializeEscalation(Deadline deadline, EscalationPolicy policy) {
        String configuredTimezone = deadline.getMatter().getOrganization().getTimezone();
        ZoneId timezone = ZoneId.of(configuredTimezone == null || configuredTimezone.isBlank() ? "UTC" : configuredTimezone);
        ZonedDateTime dueDateTime = deadline.getDueDate().atStartOfDay(timezone);
        Instant scheduled = (switch (policy.getTriggerType()) {
            case BEFORE_DUE -> dueDateTime.minusDays(policy.getTriggerOffsetDays());
            case AFTER_DUE -> dueDateTime.plusDays(policy.getTriggerOffsetDays());
        }).toInstant();
        Notification n = new Notification();
        n.setDeadline(deadline);
        n.setType(Notification.Type.ESCALATION);
        n.setScheduledFor(scheduled);
        n.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
        notificationRepository.save(n);
    }
}
