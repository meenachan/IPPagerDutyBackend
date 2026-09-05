package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import com.IpPagerDuty.ipDeadlineTracker.security.ForbiddenException;
import com.IpPagerDuty.ipDeadlineTracker.security.NotFoundException;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterCreateRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatterService {
    private final MatterRepository matterRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MatterParticipantRepository participantRepository;
    private final AuditService auditService;
    private final MatterCreationRateLimiter matterCreationRateLimiter;

    public MatterService(MatterRepository matterRepository,
                         OrganizationRepository organizationRepository,
                         OrganizationMemberRepository memberRepository,
                         UserRepository userRepository,
                         MatterParticipantRepository participantRepository,
                         AuditService auditService,
                         MatterCreationRateLimiter matterCreationRateLimiter) {
        this.matterRepository = matterRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.auditService = auditService;
        this.matterCreationRateLimiter = matterCreationRateLimiter;
    }

    @Transactional
    public Matter create(UUID orgId, MatterCreateRequest request, User user) {
        OrganizationMember member = memberRepository.findByUserId(user.getId())
            .orElseThrow(() -> new NotFoundException("organization not found"));
        if (!member.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("organization not found");
        }
        if (member.getRole() == OrganizationMember.Role.CLIENT) {
            // own matters only is allowed, so they create it as owner
        }

        Organization org = member.getOrganization();
        matterCreationRateLimiter.checkAndRecord(orgId, user.getId());
        Matter matter = new Matter();
        matter.setOrganization(org);
        matter.setTitle(request.title());
        matter.setDocketNumber(request.docketNumber());
        matter.setType(request.type());
        matter.setOwner(user);
        matter.setCreatedBy(user);
        matterRepository.save(matter);

        addWatchers(matter, request.watcherUserIds(), orgId);

        auditService.record(org, user, "MATTER", matter.getId(), "created", Map.of(
            "title", matter.getTitle(),
            "type", matter.getType().name()
        ));
        return matter;
    }

    public List<Matter> list(UUID orgId, User user) {
        OrganizationMember member = ensureMemberOf(orgId, user);
        List<Matter> all = matterRepository.findByOrganizationId(orgId);
        if (member.getRole() == OrganizationMember.Role.CLIENT) {
            return all.stream()
                .filter(m -> m.getOwner().getId().equals(user.getId()) || isWatcher(m, user))
                .toList();
        }
        return all;
    }

    @Transactional
    public void delete(UUID orgId, UUID matterId, User user) {
        Matter matter = matterRepository.findById(matterId)
            .orElseThrow(() -> new NotFoundException("matter not found"));
        OrganizationMember member = ensureMemberOf(orgId, user);
        if (!matter.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("matter not found");
        }
        if (member.getRole() != OrganizationMember.Role.BUSINESS_OWNER
            && !matter.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("only the matter owner or business owner can delete this matter");
        }
        auditService.record(matter.getOrganization(), user, "MATTER", matter.getId(), "deleted", Map.of(
            "title", matter.getTitle()
        ));
        matterRepository.delete(matter);
    }

    public Matter get(UUID matterId, User user) {
        Matter matter = matterRepository.findByIdWithParticipants(matterId)
            .orElseThrow(() -> new NotFoundException("matter not found"));
        OrganizationMember member = ensureMemberOf(matter.getOrganization().getId(), user);
        if (!canView(matter, member, user)) {
            throw new NotFoundException("matter not found");
        }
        return matter;
    }

    @Transactional
    public Matter update(UUID matterId, MatterUpdateRequest request, User user) {
        Matter matter = get(matterId, user);
        if (request.title() != null) {
            matter.setTitle(request.title());
        }
        if (request.docketNumber() != null) {
            matter.setDocketNumber(request.docketNumber());
        }
        if (request.watcherUserIds() != null) {
            participantRepository.deleteAll(matter.getParticipants());
            matter.getParticipants().clear();
            addWatchers(matter, request.watcherUserIds(), matter.getOrganization().getId());
        }
        auditService.record(matter.getOrganization(), user, "MATTER", matter.getId(), "updated", Map.of(
            "title", matter.getTitle()
        ));
        return matter;
    }

    private void addWatchers(Matter matter, Set<UUID> watcherIds, UUID orgId) {
        if (watcherIds == null) return;
        Set<UUID> orgUserIds = memberRepository.findByOrganizationId(orgId).stream()
            .map(m -> m.getUser().getId())
            .collect(Collectors.toSet());
        for (UUID uid : watcherIds) {
            if (!orgUserIds.contains(uid)) continue;
            MatterParticipant p = new MatterParticipant();
            p.setMatter(matter);
            p.setUser(userRepository.getReferenceById(uid));
            p.setAccessLevel(MatterParticipant.AccessLevel.WATCHER);
            matter.getParticipants().add(p);
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

    private boolean canView(Matter matter, OrganizationMember member, User user) {
        if (member.getRole() == OrganizationMember.Role.PARALEGAL) return true;
        return matter.getOwner().getId().equals(user.getId())
            || isWatcher(matter, user);
    }

    private boolean isWatcher(Matter matter, User user) {
        return matter.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(user.getId()));
    }
}
