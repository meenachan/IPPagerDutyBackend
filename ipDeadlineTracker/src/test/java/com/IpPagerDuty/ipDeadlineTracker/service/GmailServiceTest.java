package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.GmailProperties;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GmailServiceTest {
    @Test
    void validatesRequiredConfiguration() {
        GmailProperties properties = new GmailProperties();
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> GmailService.validateConfiguration(properties));
        assertEquals("GMAIL_CLIENT_ID must be configured", error.getMessage());
    }

    @Test
    void createsPlainTextMimeMessageWithEmailFields() throws Exception {
        MimeMessage message = GmailService.createMimeMessage(
            "sender@gmail.com", "recipient@example.com", "Subject", "Body");

        assertEquals("sender@gmail.com", message.getFrom()[0].toString());
        assertEquals("recipient@example.com", message.getRecipients(RecipientType.TO)[0].toString());
        assertEquals("Subject", message.getSubject());
        assertEquals("Body", message.getContent().toString().trim());
    }

    @Test
    void rejectsInvalidRecipient() {
        assertThrows(GmailEmailException.class,
            () -> GmailService.validateAddress("not-an-email"));
    }
}
