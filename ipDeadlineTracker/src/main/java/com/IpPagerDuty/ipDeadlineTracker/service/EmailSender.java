package com.IpPagerDuty.ipDeadlineTracker.service;

public interface EmailSender {
    void send(String to, String subject, String body);
}
