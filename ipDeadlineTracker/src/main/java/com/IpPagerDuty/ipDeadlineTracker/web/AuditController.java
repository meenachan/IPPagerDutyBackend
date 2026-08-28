package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.AuditEvent;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.AuditService;
import com.IpPagerDuty.ipDeadlineTracker.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/audit-events")
public class AuditController {
    private final AuditService auditService;
    private final OrganizationService organizationService;

    public AuditController(AuditService auditService, OrganizationService organizationService) {
        this.auditService = auditService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@PathVariable UUID orgId,
                                                    @RequestParam(required = false) String entityType,
                                                    @RequestParam(required = false) UUID entityId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        organizationService.ensureMemberOf(orgId, ctx.user());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditEvent> result = auditService.list(orgId, entityType, entityId, pageable);
        return ResponseEntity.ok(Map.of(
            "content", result.getContent(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        ));
    }
}
