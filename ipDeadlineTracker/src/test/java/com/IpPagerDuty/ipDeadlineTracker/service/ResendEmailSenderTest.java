package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ResendEmailSenderTest {

    @Test
    void constructionFailsFastWhenApiKeyMissing() {
        AppProperties props = new AppProperties();
        props.getEmail().setProvider("resend");
        // api-key left unset
        assertThrows(IllegalStateException.class, () -> new ResendEmailSender(props));
    }

    @Test
    void sendSwallowsTransportErrorsInsteadOfThrowing() {
        AppProperties props = new AppProperties();
        props.getEmail().setProvider("resend");
        props.getEmail().getResend().setApiKey("test-key");
        // Point at a base URL nothing is listening on, so the HTTP call fails fast.
        props.getEmail().getResend().setBaseUrl("http://127.0.0.1:1");
        ResendEmailSender sender = new ResendEmailSender(props);
        // Should not throw: transport failures are logged, not propagated.
        sender.send("someone@example.com", "subject", "body");
    }
}
