package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.Matter;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.MatterService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterCreateRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterResponse;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/matters")
public class MatterController {
    private final MatterService matterService;

    public MatterController(MatterService matterService) {
        this.matterService = matterService;
    }

    @PostMapping
    public ResponseEntity<MatterResponse> create(@PathVariable UUID orgId,
                                                 @Valid @RequestBody MatterCreateRequest request,
                                                 HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        Matter matter = matterService.create(orgId, request, ctx.user());
        return ResponseEntity.ok(MatterResponse.from(matter));
    }

    @GetMapping
    public ResponseEntity<List<MatterResponse>> list(@PathVariable UUID orgId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(matterService.list(orgId, ctx.user()).stream()
            .map(MatterResponse::from)
            .toList());
    }
}
