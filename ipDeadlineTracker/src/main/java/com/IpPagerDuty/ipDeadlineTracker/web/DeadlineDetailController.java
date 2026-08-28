package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.DeadlineService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineResponse;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deadlines/{deadlineId}")
public class DeadlineDetailController {
    private final DeadlineService deadlineService;

    public DeadlineDetailController(DeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @GetMapping
    public ResponseEntity<DeadlineResponse> get(@PathVariable UUID deadlineId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(DeadlineResponse.from(deadlineService.get(deadlineId, ctx.user())));
    }

    @PatchMapping
    public ResponseEntity<DeadlineResponse> update(@PathVariable UUID deadlineId,
                                                   @RequestBody DeadlineUpdateRequest request,
                                                   HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(DeadlineResponse.from(deadlineService.update(deadlineId, request, ctx.user())));
    }

    @PostMapping("/complete")
    public ResponseEntity<DeadlineResponse> complete(@PathVariable UUID deadlineId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(DeadlineResponse.from(deadlineService.complete(deadlineId, ctx.user())));
    }
}
