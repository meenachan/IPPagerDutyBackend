package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Sends transactional email via the Resend (resend.com) HTTP API instead of SMTP.
 * Activated with app.email.provider=resend (requires app.email.resend.api-key).
 * Resend has no official low-level Java SDK, so this calls the REST API directly
 * (POST https://api.resend.com/emails) rather than pulling in an unverified dependency.
 */
@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(ResendEmailSender.class);

    private final RestClient restClient;
    private final String fromAddress;

    public ResendEmailSender(AppProperties appProperties) {
        AppProperties.Email.Resend config = appProperties.getEmail().getResend();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("app.email.resend.api-key must be set when app.email.provider=resend");
        }
        this.fromAddress = config.getFromAddress();
        this.restClient = RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
            .build();
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            restClient.post()
                .uri("/emails")
                .body(Map.of(
                    "from", fromAddress,
                    "to", java.util.List.of(to),
                    "subject", subject,
                    "text", body
                ))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            logger.warn("Failed to send email via Resend to {}: {}", to, e.getMessage());
        }
    }
}
