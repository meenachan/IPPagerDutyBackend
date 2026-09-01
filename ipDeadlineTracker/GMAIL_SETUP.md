# Gmail API setup

IPPagerDuty sends mail through the Gmail API over HTTPS. It does not use SMTP, port 587, Gmail passwords, or App Passwords.

1. Create a project in Google Cloud Console and enable **Gmail API**.
2. Open **Google Auth Platform**, configure the consent screen, and add `https://www.googleapis.com/auth/gmail.send` as the only scope.
3. Create a **Web application** OAuth client.
4. Add `http://localhost:8080/oauth2/callback` as an authorized redirect URI.
5. If the app is in testing, add the sender Gmail account as a test user.
6. Set `GMAIL_CLIENT_ID` and `GMAIL_CLIENT_SECRET` locally and start the backend with `./gradlew bootRun`.
7. Visit `http://localhost:8080/oauth2/authorize` and authorize the sender account.
8. Copy the refresh token from `~/.ipdeadlinetracker/gmail-refresh-token` securely, then delete the local file. It is never returned in the HTTP response or logged.
9. Configure Railway:

   ```text
   GMAIL_CLIENT_ID=...
   GMAIL_CLIENT_SECRET=...
   GMAIL_REFRESH_TOKEN=...
   GMAIL_SENDER=sender@gmail.com
   GMAIL_REDIRECT_URI=http://localhost:8080/oauth2/callback
   ```

The deployed backend uses the refresh token non-interactively and sends through Gmail API HTTPS/443.
