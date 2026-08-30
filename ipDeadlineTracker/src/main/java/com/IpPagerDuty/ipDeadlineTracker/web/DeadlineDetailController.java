package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.DeadlineService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineNotDoneRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineResponse;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
        Deadline deadline = deadlineService.get(deadlineId, ctx.user());
        return ResponseEntity.ok(DeadlineResponse.from(deadline, deadlineService.attachedPolicies(deadlineId)));
    }

    @PatchMapping
    public ResponseEntity<DeadlineResponse> update(@PathVariable UUID deadlineId,
                                                   @RequestBody DeadlineUpdateRequest request,
                                                   HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        Deadline deadline = deadlineService.update(deadlineId, request, ctx.user());
        return ResponseEntity.ok(DeadlineResponse.from(deadline, deadlineService.attachedPolicies(deadlineId)));
    }

    @PostMapping("/complete")
    public ResponseEntity<DeadlineResponse> complete(@PathVariable UUID deadlineId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        Deadline deadline = deadlineService.complete(deadlineId, ctx.user());
        return ResponseEntity.ok(DeadlineResponse.from(deadline, deadlineService.attachedPolicies(deadlineId)));
    }

    @PostMapping("/not-done")
    public ResponseEntity<DeadlineResponse> notDone(@PathVariable UUID deadlineId,
                                                    @Valid @RequestBody DeadlineNotDoneRequest request,
                                                    HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        Deadline deadline = deadlineService.markNotDone(deadlineId, request.reason(), ctx.user());
        return ResponseEntity.ok(DeadlineResponse.from(deadline, deadlineService.attachedPolicies(deadlineId)));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID deadlineId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        deadlineService.archive(deadlineId, ctx.user());
        return ResponseEntity.noContent().build();
    }
}

