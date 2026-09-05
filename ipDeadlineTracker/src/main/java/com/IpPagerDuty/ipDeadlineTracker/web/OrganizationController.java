package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.domain.Organization;
import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.OrganizationService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.InviteMemberRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.OrganizationCreateRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.OrganizationMemberResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;
    private final List<Integer> defaultOffsets;

    public OrganizationController(OrganizationService organizationService,
                                   com.IpPagerDuty.ipDeadlineTracker.config.AppProperties appProperties) {
        this.organizationService = organizationService;
        this.defaultOffsets = appProperties.getReminders().getOffsetsDays();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody OrganizationCreateRequest request,
                                                     HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        Organization org = organizationService.create(request, ctx.user());
        return ResponseEntity.ok(Map.of("id", org.getId().toString(), "name", org.getName()));
    }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<OrganizationMemberResponse> invite(@PathVariable UUID orgId,
                                                            @Valid @RequestBody InviteMemberRequest request,
                                                            HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        OrganizationMember member = organizationService.invite(orgId, request, ctx.user());
        return ResponseEntity.ok(OrganizationMemberResponse.from(member));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<List<OrganizationMemberResponse>> list(@PathVariable UUID orgId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        return ResponseEntity.ok(organizationService.listMembers(orgId, ctx.user()).stream()
            .map(OrganizationMemberResponse::from)
            .toList());
    }

    @DeleteMapping("/{orgId}/members/{memberId}")
    public ResponseEntity<Void> remove(@PathVariable UUID orgId, @PathVariable UUID memberId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        organizationService.removeMember(orgId, memberId, ctx.user());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orgId}/notification-settings")
    public ResponseEntity<com.IpPagerDuty.ipDeadlineTracker.web.dto.NotificationSettingsResponse> getNotificationSettings(
            @PathVariable UUID orgId, HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        List<Integer> offsets = organizationService.getReminderOffsetsDays(orgId, ctx.user(), defaultOffsets);
        String timezone = organizationService.getTimezone(orgId, ctx.user());
        return ResponseEntity.ok(new com.IpPagerDuty.ipDeadlineTracker.web.dto.NotificationSettingsResponse(offsets, timezone));
    }

    @PatchMapping("/{orgId}/notification-settings")
    public ResponseEntity<com.IpPagerDuty.ipDeadlineTracker.web.dto.NotificationSettingsResponse> updateNotificationSettings(
            @PathVariable UUID orgId,
            @org.springframework.web.bind.annotation.RequestBody com.IpPagerDuty.ipDeadlineTracker.web.dto.NotificationSettingsRequest request,
            HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        List<Integer> offsets = organizationService.updateReminderOffsetsDays(orgId, request.reminderOffsetsDays(), request.timezone(), ctx.user());
        String timezone = organizationService.getTimezone(orgId, ctx.user());
        return ResponseEntity.ok(new com.IpPagerDuty.ipDeadlineTracker.web.dto.NotificationSettingsResponse(offsets, timezone));
    }
}
