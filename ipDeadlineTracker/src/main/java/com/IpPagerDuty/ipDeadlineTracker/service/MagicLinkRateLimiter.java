package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.security.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter for the magic-link request endpoint.
 * Enforces spec.md limits: 5 requests/email/hour and 20 requests/IP/hour.
 * MVP-scoped (single instance); would need a shared store (e.g. Redis) behind a
 * load balancer with multiple instances.
 */
@Component
public class MagicLinkRateLimiter {
    private static final long WINDOW_MILLIS = 60 * 60 * 1000L;
    private static final int MAX_PER_EMAIL = 5;
    private static final int MAX_PER_IP = 20;

    private final ConcurrentHashMap<String, Deque<Long>> emailHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> ipHits = new ConcurrentHashMap<>();

    public void checkAndRecord(String email, String ip) {
        long now = Instant.now().toEpochMilli();
        if (!tryConsume(emailHits, email.toLowerCase().trim(), MAX_PER_EMAIL, now)) {
            throw new TooManyRequestsException("too many magic link requests for this email, please try again later");
        }
        if (ip != null && !tryConsume(ipHits, ip, MAX_PER_IP, now)) {
            throw new TooManyRequestsException("too many magic link requests from this address, please try again later");
        }
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    void removeExpiredEntries() {
        removeExpiredEntries(emailHits);
        removeExpiredEntries(ipHits);
    }

    private void removeExpiredEntries(ConcurrentHashMap<String, Deque<Long>> store) {
        long now = Instant.now().toEpochMilli();
        store.forEach((key, hits) -> {
            synchronized (hits) {
                while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MILLIS) {
                    hits.pollFirst();
                }
                if (hits.isEmpty()) {
                    store.remove(key, hits);
                }
            }
        });
    }

    private boolean tryConsume(ConcurrentHashMap<String, Deque<Long>> store, String key, int max, long now) {
        Deque<Long> hits = store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MILLIS) {
                hits.pollFirst();
            }
            if (hits.size() >= max) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }
}
