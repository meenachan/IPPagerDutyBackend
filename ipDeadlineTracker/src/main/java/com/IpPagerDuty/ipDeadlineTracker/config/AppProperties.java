package com.IpPagerDuty.ipDeadlineTracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private MagicLink magicLink = new MagicLink();
    private Session session = new Session();
    private Reminders reminders = new Reminders();
    private Frontend frontend = new Frontend();
    private Cors cors = new Cors();
    private Email email = new Email();

    public MagicLink getMagicLink() { return magicLink; }
    public void setMagicLink(MagicLink magicLink) { this.magicLink = magicLink; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    public Reminders getReminders() { return reminders; }
    public void setReminders(Reminders reminders) { this.reminders = reminders; }

    public Frontend getFrontend() { return frontend; }
    public void setFrontend(Frontend frontend) { this.frontend = frontend; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public static class MagicLink {
        private int expiryMinutes = 15;
        public int getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
    }

    public static class Session {
        private String cookieName = "ipd_session";
        private int expiryDays = 7;
        /** Secure flag on the session cookie. Must be false for plain http://localhost dev; true in any HTTPS deployment. */
        private boolean cookieSecure = true;
        /** SameSite attribute. Use "None" (requires cookieSecure=true) when frontend/backend are on different sites. */
        private String cookieSameSite = "Lax";
        public String getCookieName() { return cookieName; }
        public void setCookieName(String cookieName) { this.cookieName = cookieName; }
        public int getExpiryDays() { return expiryDays; }
        public void setExpiryDays(int expiryDays) { this.expiryDays = expiryDays; }
        public boolean isCookieSecure() { return cookieSecure; }
        public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
        public String getCookieSameSite() { return cookieSameSite; }
        public void setCookieSameSite(String cookieSameSite) { this.cookieSameSite = cookieSameSite; }
    }

    public static class Reminders {
        private List<Integer> offsetsDays = List.of(30, 7, 2, 1);
        public List<Integer> getOffsetsDays() { return offsetsDays; }
        public void setOffsetsDays(List<Integer> offsetsDays) { this.offsetsDays = offsetsDays; }
    }

    /** Angular application base URL, used to build the magic-link callback (never the backend consume endpoint directly). */
    public static class Frontend {
        private String baseUrl = "http://localhost:4200";
        private String callbackPath = "/auth/callback";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getCallbackPath() { return callbackPath; }
        public void setCallbackPath(String callbackPath) { this.callbackPath = callbackPath; }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    /** Selects which EmailSender implementation is active: "mail" (SMTP, default) or "resend" (resend.com API). */
    public static class Email {
        private String provider = "mail";
        private Resend resend = new Resend();

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public Resend getResend() { return resend; }
        public void setResend(Resend resend) { this.resend = resend; }

        public static class Resend {
            private String apiKey;
            private String fromAddress = "IPPagerDuty <onboarding@resend.dev>";
            private String baseUrl = "https://api.resend.com";

            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getFromAddress() { return fromAddress; }
            public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        }
    }
}

