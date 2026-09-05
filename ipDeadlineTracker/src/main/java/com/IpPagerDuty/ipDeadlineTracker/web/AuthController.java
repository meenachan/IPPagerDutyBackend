package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import com.IpPagerDuty.ipDeadlineTracker.domain.Session;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.AuthService;
import com.IpPagerDuty.ipDeadlineTracker.service.MagicLinkRateLimiter;
import com.IpPagerDuty.ipDeadlineTracker.service.OrganizationService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MagicLinkConsumeRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MagicLinkRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.OrganizationCreateRequest;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.SignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AppProperties appProperties;
    private final MagicLinkRateLimiter rateLimiter;
    private final OrganizationService organizationService;

    public AuthController(AuthService authService, AppProperties appProperties, MagicLinkRateLimiter rateLimiter,
                          OrganizationService organizationService) {
        this.authService = authService;
        this.appProperties = appProperties;
        this.rateLimiter = rateLimiter;
        this.organizationService = organizationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest request,
                                                       HttpServletRequest httpRequest) {
        rateLimiter.checkAndRecord(request.email(), clientIp(httpRequest));
        com.IpPagerDuty.ipDeadlineTracker.domain.User user = authService.createUserForSignup(
            request.email(), request.displayName());
        var organization = organizationService.create(new OrganizationCreateRequest(request.organizationName(), request.role()), user);
        authService.requestMagicLink(user.getEmail());
        return ResponseEntity.accepted().body(Map.of(
            "organizationId", organization.getId().toString(),
            "message", "signup completed; check your email"
        ));
    }

    @PostMapping("/magic-link")
    public ResponseEntity<?> requestMagicLink(@Valid @RequestBody MagicLinkRequest request, HttpServletRequest httpRequest) {
        rateLimiter.checkAndRecord(request.email(), clientIp(httpRequest));
        if (!authService.requestMagicLink(request.email())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", "USER_NOT_FOUND",
                "error", "No account found for this email"
            ));
        }
        return ResponseEntity.accepted().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/magic-link/consume")
    public ResponseEntity<Map<String, String>> consumeMagicLink(@Valid @RequestBody MagicLinkConsumeRequest request,
                                                               HttpServletResponse response) {
        Session session = authService.consumeMagicLink(request.token());
        String rawToken = authService.findRawSessionToken(session);
        addSessionCookie(response, rawToken);
        return ResponseEntity.ok(Map.of("message", "logged in"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = AuthInterceptor.extractToken(request, appProperties.getSession().getCookieName());
        if (rawToken != null) {
            authService.logout(rawToken);
            clearSessionCookie(response);
        }
        return ResponseEntity.ok().build();
    }

    private void addSessionCookie(HttpServletResponse response, String rawToken) {
        AppProperties.Session session = appProperties.getSession();
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from(session.getCookieName(), rawToken)
            .httpOnly(true)
            .secure(session.isCookieSecure())
            .sameSite(session.getCookieSameSite())
            .path("/")
            .maxAge(java.time.Duration.ofDays(session.getExpiryDays()))
            .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearSessionCookie(HttpServletResponse response) {
        AppProperties.Session session = appProperties.getSession();
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from(session.getCookieName(), "")
            .httpOnly(true)
            .secure(session.isCookieSecure())
            .sameSite(session.getCookieSameSite())
            .path("/")
            .maxAge(0)
            .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
