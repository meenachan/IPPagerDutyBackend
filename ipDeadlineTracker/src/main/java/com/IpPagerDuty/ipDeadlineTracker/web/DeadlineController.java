package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.DeadlineService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineCreateRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineResponse;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.DeadlineUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matters/{matterId}/deadlines")
public class DeadlineController {
    private final DeadlineService deadlineService;

    public DeadlineController(DeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @PostMapping
    public ResponseEntity<DeadlineResponse> create(@PathVariable UUID matterId,
                                                   @Valid @RequestBody DeadlineCreateRequest request,
                                                   HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        Deadline d = deadlineService.create(matterId, request, ctx.user());
        return ResponseEntity.status(201).body(DeadlineResponse.from(d));
    }

    @GetMapping
    public ResponseEntity<List<DeadlineResponse>> list(@PathVariable UUID matterId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(deadlineService.list(matterId, ctx.user()).stream()
            .map(DeadlineResponse::from)
            .toList());
    }
}
