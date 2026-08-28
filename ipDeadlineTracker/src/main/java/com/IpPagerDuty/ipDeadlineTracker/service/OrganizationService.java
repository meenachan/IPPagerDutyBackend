package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import com.IpPagerDuty.ipDeadlineTracker.security.ConflictException;
import com.IpPagerDuty.ipDeadlineTracker.security.ForbiddenException;
import com.IpPagerDuty.ipDeadlineTracker.security.NotFoundException;
import com.IpPagerDuty.ipDeadlineTracker.security.UnauthorizedException;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.InviteMemberRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.OrganizationCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository memberRepository;
    private final MatterRepository matterRepository;
    private final DeadlineRepository deadlineRepository;
    private final AuthService authService;
    private final EmailSender emailSender;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               OrganizationMemberRepository memberRepository,
                               MatterRepository matterRepository,
                               DeadlineRepository deadlineRepository,
                               AuthService authService,
                               EmailSender emailSender,
                               AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.matterRepository = matterRepository;
        this.deadlineRepository = deadlineRepository;
        this.authService = authService;
        this.emailSender = emailSender;
        this.auditService = auditService;
    }

    @Transactional
    public Organization create(OrganizationCreateRequest request, User creator) {
        if (memberRepository.findByUserId(creator.getId()).isPresent()) {
            throw new ConflictException("user already belongs to an organization");
        }
        Organization org = new Organization();
        org.setName(request.name());
        organizationRepository.save(org);

        OrganizationMember membership = new OrganizationMember();
        membership.setOrganization(org);
        membership.setUser(creator);
        membership.setRole(OrganizationMember.Role.BUSINESS_OWNER);
        memberRepository.save(membership);

        auditService.record(org, creator, "ORGANIZATION", org.getId(), "created", Map.of("name", org.getName()));
        return org;
    }

    @Transactional
    public OrganizationMember invite(UUID orgId, InviteMemberRequest request, User inviter) {
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NotFoundException("organization not found"));
        OrganizationMember inviterMember = memberRepository.findByUserId(inviter.getId())
            .orElseThrow(UnauthorizedException::new);
        if (!inviterMember.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("organization not found");
        }
        if (inviterMember.getRole() == OrganizationMember.Role.CLIENT) {
            throw new ForbiddenException("clients cannot invite members");
        }
        if (request.role() == OrganizationMember.Role.BUSINESS_OWNER && inviterMember.getRole() != OrganizationMember.Role.BUSINESS_OWNER) {
            throw new ForbiddenException("only business owners can invite other owners");
        }

        String email = request.email().toLowerCase().trim();
        OrganizationMember existingSameOrg = memberRepository.findByOrganizationId(orgId).stream()
            .filter(m -> m.getUser().getEmail().equals(email))
            .findFirst().orElse(null);
        if (existingSameOrg != null) {
            throw new ConflictException("this email is already part of this organization", Map.of("role", existingSameOrg.getRole().name()));
        }

        OrganizationMember existingOtherOrg = memberRepository.findByUserEmail(email).orElse(null);
        if (existingOtherOrg != null && !existingOtherOrg.getOrganization().getId().equals(orgId)) {
            throw new ConflictException("this email is already part of another organization");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            return userRepository.save(u);
        });

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(user);
        member.setRole(request.role());
        memberRepository.save(member);

        authService.requestMagicLink(email);
        emailSender.send(email, "You've been invited", "You've been invited to " + org.getName());

        auditService.record(org, inviter, "ORGANIZATION_MEMBER", member.getId(), "invited", Map.of(
            "email", email,
            "role", request.role().name()
        ));
        return member;
    }

    public List<OrganizationMember> listMembers(UUID orgId, User user) {
        ensureMemberOf(orgId, user);
        return memberRepository.findStaffByOrganizationId(orgId);
    }

    @Transactional
    public void removeMember(UUID orgId, UUID memberId, User remover) {
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NotFoundException("organization not found"));
        OrganizationMember removerMember = ensureMemberOf(orgId, remover);
        if (removerMember.getRole() != OrganizationMember.Role.BUSINESS_OWNER) {
            throw new ForbiddenException("only business owners can remove members");
        }
        OrganizationMember target = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("member not found"));
        if (!target.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("member not found");
        }

        OrganizationMember fallback = memberRepository.findOwnersByOrganizationId(orgId).stream()
            .findFirst()
            .orElseThrow(() -> new ConflictException("cannot remove member; no fallback owner exists"));

        List<Deadline> openDeadlines = deadlineRepository.findOpenByResponsibleUser(orgId, target.getUser().getId());
        for (Deadline d : openDeadlines) {
            d.setResponsibleUser(fallback.getUser());
            auditService.record(org, null, "DEADLINE", d.getId(), "reassigned_due_to_member_removal", Map.of(
                "removedUser", target.getUser().getId().toString(),
                "newResponsibleUser", fallback.getUser().getId().toString()
            ));
        }

        List<Matter> ownedMatters = matterRepository.findByOrganizationId(orgId).stream()
            .filter(m -> m.getOwner().getId().equals(target.getUser().getId()))
            .toList();
        for (Matter m : ownedMatters) {
            m.setOwner(fallback.getUser());
            auditService.record(org, null, "MATTER", m.getId(), "reassigned_due_to_member_removal", Map.of(
                "removedUser", target.getUser().getId().toString(),
                "newOwner", fallback.getUser().getId().toString()
            ));
        }

        auditService.record(org, remover, "ORGANIZATION_MEMBER", target.getId(), "removed", Map.of(
            "email", target.getUser().getEmail()
        ));
        memberRepository.delete(target);
    }

    public OrganizationMember ensureMemberOf(UUID orgId, User user) {
        OrganizationMember member = memberRepository.findByUserId(user.getId())
            .orElseThrow(UnauthorizedException::new);
        if (!member.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("organization not found");
        }
        return member;
    }
}
