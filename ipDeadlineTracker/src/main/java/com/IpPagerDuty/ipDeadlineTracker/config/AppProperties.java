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

    public MagicLink getMagicLink() { return magicLink; }
    public void setMagicLink(MagicLink magicLink) { this.magicLink = magicLink; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    public Reminders getReminders() { return reminders; }
    public void setReminders(Reminders reminders) { this.reminders = reminders; }

    public static class MagicLink {
        private int expiryMinutes = 15;
        public int getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
    }

    public static class Session {
        private String cookieName = "ipd_session";
        private int expiryDays = 7;
        public String getCookieName() { return cookieName; }
        public void setCookieName(String cookieName) { this.cookieName = cookieName; }
        public int getExpiryDays() { return expiryDays; }
        public void setExpiryDays(int expiryDays) { this.expiryDays = expiryDays; }
    }

    public static class Reminders {
        private List<Integer> offsetsDays = List.of(30, 7, 2, 1);
        public List<Integer> getOffsetsDays() { return offsetsDays; }
        public void setOffsetsDays(List<Integer> offsetsDays) { this.offsetsDays = offsetsDays; }
    }
}
