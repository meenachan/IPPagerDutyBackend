package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.EscalationPolicy;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.EscalationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deadlines/{deadlineId}/escalation-policies")
public class DeadlineEscalationController {
    private final EscalationService escalationService;

    public DeadlineEscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping
    public ResponseEntity<Void> attach(@PathVariable UUID deadlineId,
                                       @RequestBody Map<String, UUID> body,
                                       HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        escalationService.attachPolicy(deadlineId, body.get("escalationPolicyId"), ctx.user());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{escalationPolicyId}")
    public ResponseEntity<Void> detach(@PathVariable UUID deadlineId,
                                       @PathVariable UUID escalationPolicyId,
                                       HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        escalationService.detachPolicy(deadlineId, escalationPolicyId, ctx.user());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<EscalationPolicy>> list(@PathVariable UUID deadlineId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(escalationService.listAttachedPolicies(deadlineId, ctx.user()));
    }
}
