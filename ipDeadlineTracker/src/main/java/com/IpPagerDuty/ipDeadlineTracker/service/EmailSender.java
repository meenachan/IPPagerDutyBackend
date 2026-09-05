package com.IpPagerDuty.ipDeadlineTracker.service;

public interface EmailSender {
    void send(String to, String subject, String body);

    default void sendHtml(String to, String subject, String textBody, String htmlBody) {
        send(to, subject, textBody);
    }
}
