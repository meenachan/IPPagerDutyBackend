package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.domain.AuditEvent;
import com.IpPagerDuty.ipDeadlineTracker.domain.Organization;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(Organization organization, User actor, String entityType, UUID entityId, String action, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent();
        event.setOrganization(organization);
        event.setActor(actor);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setAction(action);
        event.setMetadata(metadata);
        auditEventRepository.save(event);
    }

    public Page<AuditEvent> list(UUID orgId, String entityType, UUID entityId, Pageable pageable) {
        return auditEventRepository.findByOrganizationAndFilters(orgId, entityType, entityId, pageable);
    }
}
