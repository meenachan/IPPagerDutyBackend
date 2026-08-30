package com.IpPagerDuty.ipDeadlineTracker.security;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import com.IpPagerDuty.ipDeadlineTracker.domain.Session;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.OrganizationMemberRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.SessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final SessionRepository sessionRepository;
    private final OrganizationMemberRepository memberRepository;
    private final AppProperties appProperties;

    public AuthInterceptor(SessionRepository sessionRepository,
                           OrganizationMemberRepository memberRepository,
                           AppProperties appProperties) {
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String rawToken = extractToken(request, appProperties.getSession().getCookieName());
        if (rawToken == null) {
            response.setStatus(401);
            return false;
        }
        String hash = sha256(rawToken);
        Optional<Session> opt = sessionRepository.findByTokenHash(hash);
        if (opt.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        Session session = opt.get();
        Instant now = Instant.now();
        if (session.getExpiresAt().isBefore(now)) {
            response.setStatus(401);
            return false;
        }
        Instant newExpiry = now.plus(appProperties.getSession().getExpiryDays(), ChronoUnit.DAYS);
        sessionRepository.extendExpiry(session.getId(), newExpiry, now);
        session.setExpiresAt(newExpiry);

        User user = session.getUser();
        OrganizationMember membership = memberRepository.findByUserId(user.getId()).orElse(null);
        request.setAttribute("AUTH_CONTEXT", new AuthContext(user, membership));
        return true;
    }

    public static String extractToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> cookieName.equals(c.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
    }

    public static String sha256(String input) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static AuthContext require(HttpServletRequest request) {
        AuthContext ctx = (AuthContext) request.getAttribute("AUTH_CONTEXT");
        if (ctx == null) throw new UnauthorizedException();
        return ctx;
    }
}
