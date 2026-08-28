package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import com.IpPagerDuty.ipDeadlineTracker.domain.Session;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.AuthService;
import com.IpPagerDuty.ipDeadlineTracker.web.dto.MagicLinkRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @PostMapping("/magic-link")
    public ResponseEntity<Void> requestMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        authService.requestMagicLink(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/magic-link/consume")
    public ResponseEntity<Map<String, String>> consumeMagicLink(@RequestBody Map<String, String> body,
                                                               HttpServletResponse response) {
        Session session = authService.consumeMagicLink(body.get("token"));
        String rawToken = authService.findRawSessionToken(session);
        addSessionCookie(response, rawToken);
        return ResponseEntity.ok(Map.of("message", "logged in"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = AuthInterceptor.extractToken(request);
        if (rawToken != null) {
            authService.logout(rawToken);
            Cookie cookie = new Cookie(appProperties.getSession().getCookieName(), "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
        return ResponseEntity.ok().build();
    }

    private void addSessionCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(appProperties.getSession().getCookieName(), rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) ChronoUnit.SECONDS.between(java.time.Instant.now(), java.time.Instant.now().plus(7, ChronoUnit.DAYS)));
        response.addCookie(cookie);
    }

}
