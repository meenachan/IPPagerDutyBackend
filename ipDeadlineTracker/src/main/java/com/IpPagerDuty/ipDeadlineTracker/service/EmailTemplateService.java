package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class EmailTemplateService {
    private final AppProperties appProperties;

    public EmailTemplateService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String magicLinkText(String link) {
        return "Sign in to IPPagerDuty\n\nUse this secure sign-in link:\n" + link
            + "\n\nThis link expires in " + appProperties.getMagicLink().getExpiryMinutes() + " minutes.";
    }

    public String magicLinkHtml(String link) {
        return card(
            "#635bdb",
            "Sign in to IPPagerDuty",
            "Use the secure link below to access your workspace. You can close this email after signing in.",
            "Sign in securely",
            link,
            "This link expires in " + appProperties.getMagicLink().getExpiryMinutes() + " minutes. If you did not request it, you can ignore this email.");
    }

    public String invitationText(String workspaceName) {
        return "You have been invited to " + workspaceName + " on IPPagerDuty.\n\n"
            + "Use the secure sign-in email sent separately to access the workspace.";
    }

    public String invitationHtml(String workspaceName) {
        return card(
            "#635bdb",
            "You’re invited to IPPagerDuty",
            "You have been invited to join " + escape(workspaceName) + ".",
            "Open IPPagerDuty",
            appProperties.getFrontend().getBaseUrl() + "/signin",
            "Use the secure sign-in link sent separately to access your workspace.");
    }

    public String manualEscalationText(String caseTitle, String deadlineType, LocalDate dueDate, String link) {
        return "Action needed: deadline escalation\n\n"
            + "Case: " + caseTitle + "\n"
            + "Deadline: " + deadlineType + "\n"
            + "Due date: " + dueDate + "\n\n"
            + "An immediate escalation was sent for this deadline.\n"
            + "View deadline: " + link;
    }

    public String manualEscalationHtml(String caseTitle, String deadlineType, LocalDate dueDate, String link) {
        return card(
            "#b7791f",
            "Action needed: deadline escalation",
            "An immediate escalation was sent for this deadline.",
            "View deadline",
            link,
            "Case: " + caseTitle + " · Deadline: " + deadlineType + " · Due: " + dueDate);
    }

    public String deadlineUrl(UUID deadlineId) {
        return appProperties.getFrontend().getBaseUrl() + "/deadline/" + deadlineId;
    }

    private String card(String accent, String title, String message, String button, String link, String footer) {
        return """
            <div style="margin:0;background:#f7f7f8;padding:32px 16px;font-family:Arial,sans-serif;color:#17171a">
              <div style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e7e7ec;border-radius:16px;overflow:hidden">
                <div style="padding:22px 24px;color:#ffffff;background:%s;font-size:18px;font-weight:700">IPPagerDuty</div>
                <div style="padding:30px 24px">
                  <h1 style="margin:0 0 12px;font-size:24px;line-height:1.25">%s</h1>
                  <p style="margin:0 0 24px;color:#686872;line-height:1.6">%s</p>
                  <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;border-radius:999px;padding:13px 20px;font-weight:700">%s</a>
                </div>
                <div style="padding:16px 24px;border-top:1px solid #e7e7ec;color:#9696a2;font-size:12px;line-height:1.5">%s</div>
              </div>
            </div>
            """.formatted(accent, escape(title), message, escape(link), accent, escape(button), escape(footer));
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
