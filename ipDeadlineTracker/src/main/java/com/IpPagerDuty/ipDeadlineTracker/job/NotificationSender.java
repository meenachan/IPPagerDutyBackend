package com.IpPagerDuty.ipDeadlineTracker.job;

public interface NotificationSender {
    void send(String to, String subject, String body);

    default void sendHtml(String to, String subject, String textBody, String htmlBody) {
        send(to, subject, textBody);
    }
}
