package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.service.GmailOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GmailOAuthController {
    private final GmailOAuthService oauthService;

    public GmailOAuthController(GmailOAuthService oauthService) { this.oauthService = oauthService; }

    @GetMapping("/oauth2/authorize")
    public ResponseEntity<Void> authorize(HttpServletRequest request) {
        requireLocalRequest(request);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", oauthService.authorizationUrl()).build();
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<String> callback(@RequestParam String code, @RequestParam String state,
                                           HttpServletRequest request) {
        requireLocalRequest(request);
        oauthService.completeAuthorization(code, state);
        return ResponseEntity.ok("Gmail authorization completed. Copy the refresh token from ~/.ipdeadlinetracker/gmail-refresh-token into GMAIL_REFRESH_TOKEN, then remove the local token file.");
    }

    private void requireLocalRequest(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if (!request.getRemoteHost().equals("localhost")
            && !"127.0.0.1".equals(address)
            && !"0:0:0:0:0:0:0:1".equals(address)
            && !"::1".equals(address)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "OAuth setup is local-only");
        }
    }
}
