# MVP Scope (Baseline Status)

## Implemented baseline

- Auth: register/login/logout/me, password reset token flow (manual delivery), personal household.
- Records: CRUD/list/detail/archive, relationships, history, search.
- Documents/Inbox: upload/list/download/delete, record linking, Tika MIME detection/text extraction, AI suggestion review.
- Obligations: CRUD/complete/cancel/reschedule, recurrence rules, reminder dedupe/sync, in-process reminder delivery creating notifications, dashboard summary, global search.
- Household: members/invitations create/accept/revoke, owner role change/remove.
- Privacy: export data and account deletion baseline endpoints/UI.
- Security baseline: CSRF/session, Argon2id, household scoping, DTO-only, rate limit/filter headers.

## Not yet MVP-complete

- Email delivery for password reset/invitations; production notification channels.
- Distributed rate limiting, AV scanning, dependency/container scanning CI, secret-manager wiring.
- Full LGPD operational flows: correction workflow, retention policy docs, backup/restore drills.
- Production hardening and external security audit. The required MVP E2E gate is implemented and green (`frontend/e2e/core-flow.spec.ts`, CI job `e2e`).
