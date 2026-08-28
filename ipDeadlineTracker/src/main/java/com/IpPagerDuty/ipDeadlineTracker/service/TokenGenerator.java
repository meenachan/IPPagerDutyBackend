package com.IpPagerDuty.ipDeadlineTracker.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generate(int bytes) {
        byte[] token = new byte[bytes];
        random.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
