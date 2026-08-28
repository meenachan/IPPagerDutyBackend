package com.IpPagerDuty.ipDeadlineTracker.job;

import com.IpPagerDuty.ipDeadlineTracker.service.EmailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
    private final EmailSender emailSender;

    public EmailNotificationSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        emailSender.send(to, subject, body);
    }
}
