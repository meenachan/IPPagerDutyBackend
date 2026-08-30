package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import com.IpPagerDuty.ipDeadlineTracker.domain.Session;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.UserRepository;
import com.IpPagerDuty.ipDeadlineTracker.service.AuthService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/auth/dev")
public class DevAuthController {
    private final UserRepository users;
    private final AuthService authService;
    private final AppProperties properties;

    public DevAuthController(UserRepository users, AuthService authService, AppProperties properties) {
        this.users = users;
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String email = request.getOrDefault("email", "").trim().toLowerCase();
        var user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("demo user not found"));
        Session session = authService.createDevelopmentSession(user);
        var config = properties.getSession();
        ResponseCookie cookie = ResponseCookie.from(config.getCookieName(), authService.findRawSessionToken(session))
            .httpOnly(true).secure(config.isCookieSecure()).sameSite(config.getCookieSameSite())
            .path("/").maxAge(Duration.ofDays(config.getExpiryDays())).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(Map.of("email", user.getEmail()));
    }
}
