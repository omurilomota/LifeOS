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
- No antivirus integration in the upload pipeline.
- Email delivery for password reset/invitations not configured.
- No full retention/backup/restore runbook or automated drills.
- Reminder delivery scheduler is poll-based in-process; for scale move to durable queue/worker.
- External security audit still pending before MVP claim. The required E2E gate is implemented and green locally and in CI.

## Security CI gates

All workflows run on pushes and PRs to `main`/`develop` (`.github/workflows/`):

| Gate | Workflow | What it enforces |
|---|---|---|
| SAST | `codeql.yml` | CodeQL for `java-kotlin` and `javascript-typescript`; results in the Security tab |
| Dependency review | `security.yml` → `dependency-review` | PRs that **add or update** a dependency with a high/critical advisory fail (`fail-on-severity: high`) |
| npm production audit | `security.yml` → `npm-audit` | `npm audit --omit=dev --audit-level=high` on pushes/weekly schedule; reports baseline debt without blocking unrelated PRs |
| Container scan | `security.yml` → `container-scan` | Trivy config scan of `backend/Dockerfile` + image scan of the built backend image, failing on HIGH/CRITICAL findings (unfixed findings ignored) |
| Functional gate | `ci.yml` → `e2e` | Playwright core-flow smoke against PostgreSQL 16 |

Dependabot (`/.github/dependabot.yml`) keeps Maven, npm and GitHub Actions current; existing production findings are tracked there (`zod`, `react-router`) and must be cleared via the corresponding dependabot PRs.

### Exceptions policy

A gate may be bypassed only for a documented, time-boxed reason:

1. Open an issue with the finding, affected version, exploitability assessment and a `security-exception` label.
2. Get approval from a code owner (`@omurilomota`, per `CODEOWNERS`).
3. Record an expiry date and the follow-up issue in the PR description.
4. Remove the exception when the dependency is upgraded or the finding is mitigated.

Exceptions must never be used for secrets in source, authentication bypasses or tenant-isolation issues.

### Secret scanning / pre-commit guidance

- Never commit `.env`, tokens, keys or real personal data; `.env` is gitignored and `.env.example` contains placeholders only.
- Before committing, run local checks: `git diff --check` and a secret scan (`gitleaks protect --staged` or `trufflehog git file://. --only-verified`).
- Add the same scanner to your editor/git hooks (`pre-commit`) if you handle credentials locally.
- Suspected leaked secrets must be rotated immediately and reported privately per `SECURITY.md`.
