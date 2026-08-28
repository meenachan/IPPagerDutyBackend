package com.IpPagerDuty.ipDeadlineTracker.security;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
