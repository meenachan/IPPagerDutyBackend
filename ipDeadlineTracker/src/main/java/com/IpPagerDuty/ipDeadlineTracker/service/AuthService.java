package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import com.IpPagerDuty.ipDeadlineTracker.domain.MagicLinkToken;
import com.IpPagerDuty.ipDeadlineTracker.domain.Session;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.MagicLinkTokenRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.SessionRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.UserRepository;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.security.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final MagicLinkTokenRepository tokenRepository;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final EmailSender emailSender;
    private final AppProperties appProperties;
    private final Map<String, Session> rawSessionTokens = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       MagicLinkTokenRepository tokenRepository,
                       SessionRepository sessionRepository,
                       TokenGenerator tokenGenerator,
                       EmailSender emailSender,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.emailSender = emailSender;
        this.appProperties = appProperties;
    }

    @Transactional
    public boolean requestMagicLink(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        String rawToken = tokenGenerator.generate(32);
        MagicLinkToken token = new MagicLinkToken();
        token.setUser(user);
        token.setTokenHash(AuthInterceptor.sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(appProperties.getMagicLink().getExpiryMinutes(), ChronoUnit.MINUTES));
        tokenRepository.save(token);

        emailSender.send(user.getEmail(), "Your magic link",
            "Click to log in: " + appProperties.getFrontend().getBaseUrl() + appProperties.getFrontend().getCallbackPath() + "?token=" + rawToken);
        logger.info("Magic link requested for user {}", user.getId());
        return true;
    }

    @Transactional
    public User createUserForSignup(String email, String displayName) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User created = new User();
            created.setEmail(normalizedEmail);
            return userRepository.save(created);
        });
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName.trim());
        }
        return user;
    }

    @Transactional
    public Session consumeMagicLink(String rawToken) {
        String hash = AuthInterceptor.sha256(rawToken);
        MagicLinkToken token = tokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new UnauthorizedException("invalid or expired link"));
        Instant now = Instant.now();
        int updated = tokenRepository.markUsed(token.getId(), now);
        if (updated == 0) {
            throw new UnauthorizedException("invalid or expired link");
        }
        Session session = new Session();
        session.setUser(token.getUser());
        String sessionToken = tokenGenerator.generate(32);
        session.setTokenHash(AuthInterceptor.sha256(sessionToken));
        session.setExpiresAt(now.plus(appProperties.getSession().getExpiryDays(), ChronoUnit.DAYS));
        sessionRepository.save(session);
        rawSessionTokens.put(sessionToken, session);
        return session;
    }

    @Transactional
    public Session createDevelopmentSession(User user) {
        Session session = new Session();
        session.setUser(user);
        String sessionToken = tokenGenerator.generate(32);
        session.setTokenHash(AuthInterceptor.sha256(sessionToken));
        session.setExpiresAt(Instant.now().plus(appProperties.getSession().getExpiryDays(), ChronoUnit.DAYS));
        sessionRepository.save(session);
        rawSessionTokens.put(sessionToken, session);
        return session;
    }

    @Transactional
    public void logout(String rawToken) {
        sessionRepository.findByTokenHash(AuthInterceptor.sha256(rawToken)).ifPresent(sessionRepository::delete);
    }

    public String findRawSessionToken(Session session) {
        for (Map.Entry<String, Session> e : rawSessionTokens.entrySet()) {
            if (e.getValue().getId().equals(session.getId())) {
                return e.getKey();
            }
        }
        return null;
    }
}
