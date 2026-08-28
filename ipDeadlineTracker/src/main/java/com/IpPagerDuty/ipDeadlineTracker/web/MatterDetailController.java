package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.Matter;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.MatterService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterResponse;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MatterUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matters/{matterId}")
public class MatterDetailController {
    private final MatterService matterService;

    public MatterDetailController(MatterService matterService) {
        this.matterService = matterService;
    }

    @GetMapping
    public ResponseEntity<MatterResponse> get(@PathVariable UUID matterId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(MatterResponse.from(matterService.get(matterId, ctx.user())));
    }

    @PatchMapping
    public ResponseEntity<MatterResponse> update(@PathVariable UUID matterId,
                                                 @RequestBody MatterUpdateRequest request,
                                                 HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(MatterResponse.from(matterService.update(matterId, request, ctx.user())));
    }
}
