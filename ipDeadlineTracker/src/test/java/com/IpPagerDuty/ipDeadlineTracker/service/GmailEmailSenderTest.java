package com.IpPagerDuty.ipDeadlineTracker.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GmailEmailSenderTest {
    @Test
    void mapsEmailFieldsAndHandlesGmailFailure() {
        GmailService gmailService = mock(GmailService.class);
        doThrow(new GmailEmailException("Gmail API request failed"))
            .when(gmailService).send("to@example.com", "Subject", "Body");

        GmailEmailSender sender = new GmailEmailSender(gmailService);
        sender.send("to@example.com", "Subject", "Body");

        verify(gmailService).send("to@example.com", "Subject", "Body");
    }
}
