package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.security.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MagicLinkRateLimiterTest {

    @Test
    void allowsUpToFivePerEmailPerHour() {
        MagicLinkRateLimiter limiter = new MagicLinkRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAndRecord("a@example.com", "10.0.0." + i);
        }
        assertThrows(TooManyRequestsException.class,
            () -> limiter.checkAndRecord("a@example.com", "10.0.0.99"));
    }

    @Test
    void allowsUpToTwentyPerIpPerHour() {
        MagicLinkRateLimiter limiter = new MagicLinkRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAndRecord("user" + i + "@example.com", "10.0.0.1");
        }
        assertThrows(TooManyRequestsException.class,
            () -> limiter.checkAndRecord("another@example.com", "10.0.0.1"));
    }

    @Test
    void differentEmailsAndIpsAreIndependentlyTracked() {
        MagicLinkRateLimiter limiter = new MagicLinkRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAndRecord("first@example.com", "10.0.0.1");
        }
        // different email, different IP -> should not be limited
        limiter.checkAndRecord("second@example.com", "10.0.0.2");
    }
}
