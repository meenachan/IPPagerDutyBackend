package com.IpPagerDuty.ipDeadlineTracker.service;

public class GmailEmailException extends RuntimeException {
    public GmailEmailException(String message) { super(message); }
    public GmailEmailException(String message, Throwable cause) { super(message, cause); }
}
