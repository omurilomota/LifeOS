# Security & Privacy Baseline

## Implemented

- Server-side session auth, CSRF cookie/header for browser flows.
- Argon2id password hashing.
- Household-scoped queries on tenant-owned endpoints; 404/403 patterns for unauthorized access.
- DTO-only API responses; no JPA entity exposure.
- Upload baseline: size limit, allowed type checks, safe storage key, authorized download.
- Tika-based MIME detection/text extraction baseline.
- AI disabled by default; suggestions require explicit enable and human confirm/reject; audit events recorded.
- Basic in-memory rate limiting filter (per-IP window).
- Security headers: X-Content-Type-Options, frame deny, referrer no-referrer, restrictive permissions policy.
- LGPD baseline: export endpoint (`GET /api/v1/privacy/export`) and account deletion endpoint (`DELETE /api/v1/privacy/account`) with confirmation in UI.

## Known gaps (pre-production hardening)

- Rate limiting is in-memory/single-node; use distributed limiter for multi-instance production.
- No antivirus integration, no dependency/container scanning pipeline wired in CI here.
- Email delivery for password reset/invitations not configured.
- No full retention/backup/restore runbook or automated drills.
- Reminder delivery scheduler is poll-based in-process; for scale move to durable queue/worker.
- Required full E2E gate and external security audit still pending before MVP claim.
