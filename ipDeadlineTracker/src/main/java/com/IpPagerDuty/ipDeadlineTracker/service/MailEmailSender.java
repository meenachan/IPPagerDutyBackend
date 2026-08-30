package com.IpPagerDuty.ipDeadlineTracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "mail", matchIfMissing = true)
public class MailEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(MailEmailSender.class);
    private final JavaMailSender mailSender;
    private final String from;

    public MailEmailSender(JavaMailSender mailSender, @Value("${spring.mail.username:noreply@example.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
