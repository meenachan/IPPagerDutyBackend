package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationEmailGroup;
import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationPolicy;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.EscalationService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.EscalationEmailGroupRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.EscalationPolicyRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}")
public class EscalationController {
    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping("/escalation-email-groups")
    public ResponseEntity<Map<String, Object>> createGroup(@PathVariable UUID orgId,
                                                           @Valid @RequestBody EscalationEmailGroupRequest request,
                                                           HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        EscalationEmailGroup group = escalationService.createEmailGroup(orgId, request, ctx.user());
        return ResponseEntity.status(201).body(Map.of("id", group.getId().toString(), "name", group.getName(), "emails", group.getEmails()));
    }

    @GetMapping("/escalation-email-groups")
    public ResponseEntity<List<EscalationEmailGroup>> listGroups(@PathVariable UUID orgId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(escalationService.listEmailGroups(orgId, ctx.user()));
    }

    @DeleteMapping("/escalation-email-groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID orgId, @PathVariable UUID groupId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        escalationService.deleteEmailGroup(orgId, groupId, ctx.user());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/escalation-policies")
    public ResponseEntity<Map<String, Object>> createPolicy(@PathVariable UUID orgId,
                                                            @Valid @RequestBody EscalationPolicyRequest request,
                                                            HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        EscalationPolicy policy = escalationService.createPolicy(orgId, request, ctx.user());
        return ResponseEntity.status(201).body(Map.of(
            "id", policy.getId().toString(),
            "name", policy.getName(),
            "triggerType", policy.getTriggerType().name(),
            "triggerOffsetDays", policy.getTriggerOffsetDays(),
            "emailGroupId", policy.getEmailGroup().getId().toString()
        ));
    }

    @GetMapping("/escalation-policies")
    public ResponseEntity<List<EscalationPolicy>> listPolicies(@PathVariable UUID orgId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(escalationService.listPolicies(orgId, ctx.user()));
    }
}
