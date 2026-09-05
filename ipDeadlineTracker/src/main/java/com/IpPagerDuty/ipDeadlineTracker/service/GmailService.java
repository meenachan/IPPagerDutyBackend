package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.GmailProperties;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Service
public class GmailService {
    private static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";
    private final GmailProperties properties;
    private Gmail gmail;

    public GmailService(GmailProperties properties) {
        this.properties = properties;
    }

    private Gmail gmail() {
        if (gmail != null) return gmail;
        validateConfiguration(properties);
        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credentials = UserCredentials.newBuilder()
                .setClientId(properties.getClientId())
                .setClientSecret(properties.getClientSecret())
                .setRefreshToken(properties.getRefreshToken())
                .build()
                .createScoped(List.of(GMAIL_SEND_SCOPE));
            gmail = new Gmail.Builder(transport, GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("IPPagerDuty")
                .build();
            return gmail;
        } catch (Exception e) {
            throw new GmailEmailException("Unable to initialize Gmail API client", e);
        }
    }

    public void send(String to, String subject, String body) {
        try {
            sendMime(to, subject, createMimeMessage(properties.getSender(), to, subject, body));
        } catch (AddressException e) {
            throw new GmailEmailException("Invalid recipient email address", e);
        } catch (jakarta.mail.MessagingException e) {
            throw new GmailEmailException("Gmail message creation failed", e);
        }
    }

    public void sendHtml(String to, String subject, String textBody, String htmlBody) {
        validateAddress(to);
        try {
            MimeMessage mimeMessage = createHtmlMimeMessage(properties.getSender(), to, subject, textBody, htmlBody);
            sendMime(to, subject, mimeMessage);
        } catch (AddressException e) {
            throw new GmailEmailException("Invalid recipient email address", e);
        } catch (jakarta.mail.MessagingException e) {
            throw new GmailEmailException("Gmail API request failed", e);
        }
    }

    private void sendMime(String to, String subject, MimeMessage mimeMessage) {
        validateAddress(to);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mimeMessage.writeTo(output);
            Message message = new Message().setRaw(Base64.getUrlEncoder().withoutPadding()
                .encodeToString(output.toByteArray()));
            gmail().users().messages().send("me", message).execute();
        } catch (AddressException e) {
            throw new GmailEmailException("Invalid recipient email address", e);
        } catch (IOException | jakarta.mail.MessagingException e) {
            throw new GmailEmailException("Gmail API request failed", e);
        }
    }

    static MimeMessage createHtmlMimeMessage(String sender, String to, String subject, String textBody, String htmlBody)
        throws AddressException, jakarta.mail.MessagingException {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(RecipientType.TO, new Address[]{new InternetAddress(to, true)});
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        MimeMultipart multipart = new MimeMultipart("alternative");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(textBody, StandardCharsets.UTF_8.name());
        multipart.addBodyPart(textPart);
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);
        return message;
    }

    static MimeMessage createMimeMessage(String sender, String to, String subject, String body)
        throws AddressException, jakarta.mail.MessagingException {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(RecipientType.TO, new Address[]{new InternetAddress(to, true)});
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(body, StandardCharsets.UTF_8.name());
        return message;
    }

    static void validateConfiguration(GmailProperties properties) {
        require(properties.getClientId(), "GMAIL_CLIENT_ID");
        require(properties.getClientSecret(), "GMAIL_CLIENT_SECRET");
        require(properties.getRefreshToken(), "GMAIL_REFRESH_TOKEN");
        require(properties.getSender(), "GMAIL_SENDER");
        require(properties.getRedirectUri(), "GMAIL_REDIRECT_URI");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be configured");
    }

    static void validateAddress(String address) {
        try {
            InternetAddress parsed = new InternetAddress(address, true);
            parsed.validate();
        } catch (AddressException e) {
            throw new GmailEmailException("Invalid recipient email address", e);
        }
    }
}
