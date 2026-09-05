package com.IpPagerDuty.ipDeadlineTracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GmailEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(GmailEmailSender.class);
    private final GmailService gmailService;

    public GmailEmailSender(GmailService gmailService) { this.gmailService = gmailService; }

    @Override
    public void send(String to, String subject, String body) {
        try {
            gmailService.send(to, subject, body);
        } catch (GmailEmailException | IllegalStateException e) {
            logger.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendHtml(String to, String subject, String textBody, String htmlBody) {
        try {
            gmailService.sendHtml(to, subject, textBody, htmlBody);
        } catch (GmailEmailException | IllegalStateException e) {
            logger.warn("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }
}
