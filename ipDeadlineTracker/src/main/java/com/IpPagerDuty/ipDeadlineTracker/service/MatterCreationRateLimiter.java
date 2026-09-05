package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.security.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents accidental duplicate matter creation from rapid repeated requests.
 * This is intentionally short-lived; it is not a quota for normal matter creation.
 */
@Component
public class MatterCreationRateLimiter {
    private static final Duration WINDOW = Duration.ofSeconds(2);

    private final ConcurrentHashMap<String, Instant> lastRequests = new ConcurrentHashMap<>();

    public synchronized void checkAndRecord(UUID organizationId, UUID userId) {
        String key = organizationId + ":" + userId;
        Instant now = Instant.now();
        Instant previous = lastRequests.get(key);
        if (previous != null && now.isBefore(previous.plus(WINDOW))) {
            throw new TooManyRequestsException("please wait before creating another matter");
        }
        lastRequests.put(key, now);
    }
}
