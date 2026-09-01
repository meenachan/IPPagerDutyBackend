package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.GmailProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;

@Service
public class GmailOAuthService {
    private static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";
    private final GmailProperties properties;
    private String expectedState;

    public GmailOAuthService(GmailProperties properties) {
        this.properties = properties;
    }

    public String authorizationUrl() {
        validateOAuthConfiguration(properties);
        byte[] stateBytes = new byte[32];
        new SecureRandom().nextBytes(stateBytes);
        expectedState = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        return flow().newAuthorizationUrl().setRedirectUri(properties.getRedirectUri())
            .setAccessType("offline").setApprovalPrompt("force").setState(expectedState).build();
    }

    public void completeAuthorization(String code, String state) {
        if (code == null || code.isBlank() || state == null || !state.equals(expectedState)) {
            throw new GmailEmailException("Invalid OAuth callback state or authorization code");
        }
        expectedState = null;
        try {
            GoogleTokenResponse response = flow().newTokenRequest(code)
                .setRedirectUri(properties.getRedirectUri()).execute();
            String refreshToken = response.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new GmailEmailException("Google did not return a refresh token; revoke prior consent and try again");
            }
            writeLocalRefreshToken(refreshToken);
        } catch (IOException e) {
            throw new GmailEmailException("Google OAuth token exchange failed", e);
        }
    }

    private GoogleAuthorizationCodeFlow flow() {
        try {
            return new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), properties.getClientId(), properties.getClientSecret(),
                List.of(GMAIL_SEND_SCOPE)).setAccessType("offline").build();
        } catch (GeneralSecurityException | IOException e) {
            throw new GmailEmailException("Unable to initialize Google OAuth client", e);
        }
    }

    private static void writeLocalRefreshToken(String refreshToken) throws IOException {
        Path directory = Path.of(System.getProperty("user.home"), ".ipdeadlinetracker");
        Files.createDirectories(directory);
        Path tokenFile = directory.resolve("gmail-refresh-token");
        Files.writeString(tokenFile, refreshToken, StandardCharsets.UTF_8);
        try {
            Files.setPosixFilePermissions(tokenFile, EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    static void validateOAuthConfiguration(GmailProperties properties) {
        if (properties.getClientId() == null || properties.getClientId().isBlank()
            || properties.getClientSecret() == null || properties.getClientSecret().isBlank()
            || properties.getRedirectUri() == null || properties.getRedirectUri().isBlank()) {
            throw new IllegalStateException("GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET and GMAIL_REDIRECT_URI must be configured");
        }
    }
}
