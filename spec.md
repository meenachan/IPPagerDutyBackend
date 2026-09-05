# Spec: IP PagerDuty — MVP

## Summary
IP PagerDuty is a passwordless, multi-tenant SaaS that gives small IP
practices and business owners a lightweight accountability layer for IP
deadlines: every deadline has a clear owner, automated reminders, an
escalation path if nobody acts, and a transparent, immutable history of
who knew what and when. This spec covers the REST JSON API backend
(Java 21 + Spring Boot) for the MVP only — no UI is built in this repo.

## Domain model

| Entity | Fields (key ones) | Notes |
|---|---|---|
| `Organization` | id, name, createdAt | Single tenant boundary. |
| `User` | id, email (unique), createdAt | No password ever stored. |
| `OrganizationMember` | id, organizationId, userId, role | Role ∈ `BUSINESS_OWNER`, `LAWYER`, `PARALEGAL`, `CLIENT`. **One user belongs to exactly one organization in MVP** — a second signup attempt with an email already tied to an org is rejected (see Edge cases). |
| `Matter` | id, organizationId, title, type (`PATENT`/`TRADEMARK`/`OTHER`), ownerUserId, createdBy, createdAt | |
| `MatterParticipant` | matterId, userId, accessLevel (`OWNER`/`WATCHER`) | Who besides the owner can see/act on a matter. |
| `Deadline` | id, matterId, dueDate, type, responsibleUserId, status, createdBy, createdAt, completedAt | status ∈ `OPEN`, `COMPLETED`, `MISSED`. |
| `DeadlineWatcher` | deadlineId, userId | Extra people who see the deadline besides the responsible user. |
| `EscalationPolicy` | id, organizationId, name, triggerType (`BEFORE_DUE`/`AFTER_DUE`), triggerOffsetDays, emailGroupId | Reusable, org-scoped. |
| `DeadlineEscalationPolicy` | deadlineId, escalationPolicyId | Join table — **a deadline may attach any number of escalation policies** (e.g. a "7 days before" one and a separate "3 days after" one active at once). Each attached policy fires independently on its own trigger. |
| `EscalationEmailGroup` | id, organizationId, name, emails (list) | Named group of raw email addresses (not necessarily registered `User`s), reusable across policies. |
| `Notification` | id, deadlineId, type (`REMINDER`/`ESCALATION`), scheduledFor, sentAt, deliveryStatus (`PENDING`/`SENT`/`FAILED`) | One row per scheduled send; idempotency key = `(deadlineId, type, scheduledFor)`. |
| `AuditEvent` | id, organizationId, actorUserId (nullable for system events), entityType, entityId, action, metadata (JSON), createdAt | Immutable; append-only. |
| `MagicLinkToken` | id, userId, tokenHash, expiresAt, usedAt | Single-use, 15-minute expiry. |
| `Session` | id, userId, tokenHash, expiresAt | 7-day sliding-expiry browser session. |

## Roles and permissions

| Action | BUSINESS_OWNER | LAWYER | PARALEGAL | CLIENT |
|---|---|---|---|---|
| Create matter | Yes | Yes | Yes | Yes (own matters only, see below) |
| Create deadline | Yes | Yes | Yes | Yes (own matters only) |
| Assign responsible person | Yes | Yes | Yes | No |
| Mark deadline complete | Yes (if owner/responsible) | Yes (if responsible) | Yes (if responsible) | No |
| Bulk CSV import of deadlines | Yes | Yes | Yes | Yes (own matters only) |
| Manage escalation policies / email groups | Yes | Yes | No | No |
| View matter | If owner/watcher | If owner/watcher | If in org | Read-only, own matters only |
| View audit log | Yes (org-wide) | Own matters | Own matters | Own matters, read-only |

Clients can create matters and deadlines (including via CSV import) for
their own matters, but cannot assign responsibility to others, cannot
mark items complete, and cannot configure escalation policies.

## API surface (REST JSON, all under `/api/v1`)

### Auth
- `POST /auth/magic-link` — body `{ email }`. Returns `202 Accepted`
  and sends a magic-link email when the email belongs to an existing
  user. Returns `404 Not Found` with `{ "code": "USER_NOT_FOUND",
  "error": "No account found for this email" }` when the email is
  unknown, so the client can direct the user to create a workspace.
- `POST /auth/magic-link/consume` — body `{ token }`. Validates token
  (exists, unexpired, unused), marks it used, creates a `Session`,
  returns session cookie (`HttpOnly`, `Secure`, `SameSite=Lax`) valid 7
  days sliding.
- `POST /auth/logout` — invalidates current session.

### Organizations & members
- `POST /organizations` — creates an organization with the caller as
  `BUSINESS_OWNER`. (Bootstrapping path; see Open Questions on invite
  flow for subsequent members.)
- `POST /organizations/{orgId}/members` — invites a user by email with
  a role. Creates the `User` if new, sends them a magic link, creates
  the `OrganizationMember` row. Rejected if that email is already a
  member of a **different** organization (see Edge cases).
- `GET /organizations/{orgId}/members` — list members (staff roles only).
- `DELETE /organizations/{orgId}/members/{memberId}` — removes a member; see reassignment behavior in Edge cases.

### Matters
- `POST /organizations/{orgId}/matters`
- `GET /organizations/{orgId}/matters` — filtered to what the caller can see.
- `GET /matters/{matterId}`
- `PATCH /matters/{matterId}` — update title/participants.

### Deadlines
- `POST /matters/{matterId}/deadlines`
- `GET /matters/{matterId}/deadlines`
- `PATCH /deadlines/{deadlineId}` — update due date/responsible person/status.
- `POST /deadlines/{deadlineId}/complete` — marks `COMPLETED`, sets `completedAt`, writes audit event.
- `POST /matters/{matterId}/deadlines/import` — multipart CSV upload (see CSV import below).

### Escalation policies & email groups
- `POST /organizations/{orgId}/escalation-email-groups`
- `GET /organizations/{orgId}/escalation-email-groups`
- `POST /organizations/{orgId}/escalation-policies`
- `GET /organizations/{orgId}/escalation-policies`
- `POST /deadlines/{deadlineId}/escalation-policies` — attach a policy (body `{ escalationPolicyId }`); a deadline may have any number attached.
- `DELETE /deadlines/{deadlineId}/escalation-policies/{escalationPolicyId}` — detach one.
- `GET /deadlines/{deadlineId}/escalation-policies` — list all attached to a deadline.

### Audit
- `GET /organizations/{orgId}/audit-events` — paginated, filterable by entity.

## CSV bulk import

- Endpoint: `POST /matters/{matterId}/deadlines/import`, `multipart/form-data`, one file field `file`.
- Expected columns (header row required, case-insensitive match): `due_date` (ISO `YYYY-MM-DD`), `type`, `responsible_email` (optional — defaults to uploader), `notes` (optional).
- Max file size: 2 MB / 500 rows, whichever is smaller.
- Processing is **all-or-nothing per file**: the whole file is validated first; if any row fails validation, zero rows are imported and the response lists every failing row with its error (row number + reason).
- On success: `201 Created` with `{ importedCount, deadlineIds: [...] }`; one `AuditEvent` per created deadline plus one summary `AuditEvent` for the import batch itself.
- `responsible_email` must belong to an existing member of the organization; unknown emails are a validation failure for that row (not silently created as a new user), since CSV import is not an invite mechanism.

## Reminders & escalation processing

- Reminder schedule is fixed for MVP: 30, 7, 2, and 1 day(s) before `dueDate`, for every deadline in every organization — not configurable per org/matter/deadline in this release.
- A persistent scheduled job runs at least hourly, computing which `Notification` rows are due (`scheduledFor <= now AND deliveryStatus = PENDING`) and dispatching them via the `NotificationSender` interface (Resend-backed).
- Job execution is idempotent: `Notification` rows are pre-materialized when a `Deadline` is created (one row per reminder offset), and the send step is guarded by `(deadlineId, type, scheduledFor)` uniqueness plus a DB-level "claim" (e.g. `UPDATE ... WHERE deliveryStatus = 'PENDING'` with optimistic locking) so a retry or duplicate job run cannot double-send.
- Escalation trigger is configurable per `EscalationPolicy`: `BEFORE_DUE` (fires `triggerOffsetDays` before `dueDate` if the deadline is still `OPEN`) or `AFTER_DUE` (fires `triggerOffsetDays` after `dueDate` if still `OPEN`, i.e. overdue). A deadline may attach **zero, one, or many** `EscalationPolicy` rows via `DeadlineEscalationPolicy`; each attached policy is evaluated and fires independently, so a deadline can have both a "7 days before" and a separate "3 days after" escalation active simultaneously. If none is attached, no escalation ever fires for it (only the fixed reminders do).
- Escalation delivers to every email address in each firing policy's `EscalationEmailGroup` — these need not be registered platform users. If multiple attached policies fire on the same day for the same deadline, each still sends its own notification (no de-duplication across policies — different groups may need different messaging).
- Once a `Deadline` transitions to `COMPLETED`, all its future `Notification` rows (reminder and escalation) are cancelled (`deliveryStatus` set to a terminal `CANCELLED` state) so nothing fires after completion.
- Any `Deadline` still `OPEN` after its `dueDate` has passed is automatically transitioned to `MISSED` by the same scheduled job (this does not cancel pending `AFTER_DUE` escalations — those still fire on a `MISSED` deadline until someone completes it).

## Auth model details

- Passwordless only — no password field exists anywhere.
- Magic-link tokens: single-use, 15-minute expiry, stored hashed (never the raw token) at rest.
- Sessions: 7-day expiry, sliding (each authenticated request extends `expiresAt` by 7 days from now), stored as hashed tokens in an `HttpOnly` cookie.
- No self-serve org signup beyond bootstrapping the first organization — subsequent users are added via the invite endpoint (`POST /organizations/{orgId}/members`), consistent with "one user = one organization" (see Edge cases).

## Edge cases & error handling

- **Requesting a magic link for an unknown email:** respond `404 Not Found` with code `USER_NOT_FOUND` (no email sent, no user created), allowing the client to direct the user to signup or create a workspace.
- **Consuming an expired or already-used token:** `401 Unauthorized`, generic "invalid or expired link" message.
- **Consuming a token twice concurrently (race):** the second attempt must fail — token consumption is an atomic compare-and-set on `usedAt IS NULL`.
- **Inviting an email already belonging to a different organization:** `409 Conflict` — "this email is already part of another organization" (enforced by the one-user-one-org rule).
- **Inviting an email already in the *same* organization:** `409 Conflict` with the existing role, no duplicate row created.
- **CSV import with any invalid row:** `422 Unprocessable Entity`, full list of row-level errors, zero rows imported.
- **CSV import file too large / too many rows:** `413 Payload Too Large` before any parsing.
- **Marking a deadline complete that's already `COMPLETED` or `MISSED`:** idempotent no-op if already `COMPLETED` (`200 OK`, no new audit event); allowed transition from `MISSED` → `COMPLETED` (late completion is still tracked).
- **Escalation email group referenced by a policy, then deleted:** deletion is blocked (`409 Conflict`) while any policy still references it; policy must be updated/deleted first.
- **Removing a member who is still `responsibleUserId` on one or more `OPEN` deadlines, or `ownerUserId`/watcher on matters:** removal is **not blocked**. Instead, every such deadline's `responsibleUserId` (and every matter's `ownerUserId`) is automatically reassigned to the organization's `BUSINESS_OWNER` (if multiple exist, the one who created the organization) as a fallback owner, so nothing is left unowned. Each reassignment writes its own `AuditEvent` (`action: "reassigned_due_to_member_removal"`, metadata capturing the removed user and the new fallback owner) so the reason is visible in history. The member row is then deleted.
- **Client role attempting to assign a responsible person or configure escalation:** `403 Forbidden`.
- **Concurrent edits to the same deadline (e.g. two staff both reassign it):** last-write-wins at the row level; every write still produces its own `AuditEvent`, so the overwritten change remains visible in history even though it's no longer the active value.
- **Scheduled job crash mid-run:** safe to re-run — see idempotency guarantee above; no duplicate sends.

## Auth / permissions
See "Roles and permissions" table and "Auth model details" above. Every
endpoint requires an authenticated session except `POST
/auth/magic-link` and `POST /auth/magic-link/consume`. All
organization-scoped endpoints enforce tenant isolation: a session tied
to org A can never read or write org B's data, returning `404 Not
Found` (not `403`) for cross-tenant access attempts, so as not to
confirm the existence of resources in another tenant.

## Constraints

- CSV import: max 2 MB or 500 rows per file, all-or-nothing.
- Magic-link token: 15-minute expiry, single use.
- Session: 7-day sliding expiry.
- Reminder schedule fixed at 30/7/2/1 days before due date, not configurable in MVP.
- Rate limiting: `POST /auth/magic-link` limited to 5 requests per email per hour, and 20 requests per IP per hour, to prevent email-bombing.
- Audit events are retained indefinitely — no automatic purge in MVP.
- No sensitive data (tokens, emails in bulk) written to application logs.
- Database: PostgreSQL, Flyway-managed migrations; Hibernate in validate-only mode against production schema.

## Assumptions

- Assumed `202 Accepted` (not `200`) for the magic-link request endpoint, to signal "processing, no confirmation of outcome given."
- Assumed cross-tenant access returns `404` rather than `403` (standard tenant-isolation practice to avoid confirming resource existence) — not explicitly specified.
- Assumed the first organization is created via a simple `POST /organizations` bootstrap endpoint by whoever signs up first as `BUSINESS_OWNER`; the pitch didn't specify how the very first organization/account gets created.
- Assumed CSV (not Excel `.xlsx`) parsing for MVP per your answer; column names and exact validation rules above are a reasonable default, not specified in the pitch — flagged for your review.
- Assumed `responsible_email` in a CSV row must already be an org member, rather than silently inviting new users via import.
- Assumed escalation "before due" and "after due" can coexist per policy via `triggerType`, since you asked for both to be supported.
- Assumed "move it to the org" means reassigning a removed member's owned/responsible items to the organization's `BUSINESS_OWNER` (the org creator, if more than one exists) as fallback — not deleting the items or leaving them unowned.

## Open Questions / Risks

- **Excel `.xlsx` import** was explicitly deferred to a later spec per your answer — noted here so it isn't silently forgotten.
