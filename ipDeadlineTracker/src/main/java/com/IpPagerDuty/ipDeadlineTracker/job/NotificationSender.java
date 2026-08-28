package com.IpPagerDuty.ipDeadlineTracker.job;

public interface NotificationSender {
    void send(String to, String subject, String body);
}
