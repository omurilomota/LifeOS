# LifeOS — Master Product, Architecture, and Implementation Plan

## Status
- **Plan type**: Implementation-ready master plan for a new product in an empty workspace.
- **Workspace**: LifeOS repository bootstrapped on 2026-11-21.
- **Toolchain verified (2026-11-21)**: Java 21.0.13.1, javac 21.0.13.1, Maven 3.9.12, Node v24.20.0, npm 12.2.2, Docker 29.10.0, Docker Compose v5.8.0.
- **Current implementation status**: Phase 0-7 baseline+upgrades completed; pending-gap pass added reminder delivery scheduler, rate limiting/security headers, LGPD export/delete baseline, settings privacy UI, and tests. Backend+frontend tests/builds passing. Docker Postgres runtime smoke blocked in this environment (no docker socket).
- **Direction**: Start with **Phase 1 foundation first** (executable base), then deliver MVP through Phases 2–4 in incremental vertical slices. Do not start with AI. Do not build disconnected modules.

## Execution Log

| Date | Phase | Status | Notes |
|---|---|---|---|
| 2026-11-21 | Phase 0 - Bootstrap | Completed | Monorepo skeleton, docs, AGENTS/README/.env.example, Docker Compose, ADR 0001. |
| 2026-11-21 | Phase 1 - Backend foundation | Completed (baseline) | Spring Boot 3.3.5/Java 21, Flyway, session+CSRF, Argon2id, register/login/logout/me, password reset request/confirm (token persisted; manual/dev delivery), personal household auto-create, current household endpoint, health, error envelope. `mvn test` passed. |
| 2026-11-21 | Phase 1 - Frontend foundation | Completed (baseline) | Vite React TS Tailwind app shell, auth context, login/register, protected routes, responsive nav, Today + placeholder module routes. `npm run typecheck`, `npm run test`, `npm run lint`, `npm run build` passed. |
| 2026-11-21 | Phase 1 - Local runtime verification | Partial | Docker CLI present but daemon socket unavailable (`/var/run/docker.sock` missing); `docker compose up -d postgres` could not run. |
| 2026-11-21 | Phase 2 - Records | Completed (baseline) | Added records + relationships migration, record/audit domain/services/controllers, household-scoped CRUD/search/archive, relationships, history endpoint. Frontend records list/create/filter/archive/detail/edit/history/relationship UI. Backend tests + frontend typecheck/lint/test/build passing. |
| 2026-11-21 | Phase 3 - Documents & Inbox | Completed (baseline) | Added V4 documents/record_documents/extraction_jobs migration, storage abstraction with local implementation, document upload/list/get/download/delete, record linking, basic extraction job/status for txt files, household-scoped authz. Frontend Inbox/Documents UI with upload/filter/download/link/delete. Backend tests + frontend typecheck/lint/test/build passing. |
| 2026-11-21 | Phase 4 - Obligations, Dashboard, Search | Completed (baseline) | Added V5 obligations/reminders/notifications migration, obligation CRUD/complete/cancel, in-app notification creation, dashboard summary endpoint, grouped global search endpoint. Frontend obligations page, Today dashboard data, search page/nav bar. Backend tests + frontend typecheck/lint/test/build passing. |
| 2026-11-21 | Phase 5 - AI Suggestions | Completed (baseline) | Added V6 ai_suggestions migration, AI config disabled by default, suggestion generation/list/confirm/reject endpoints, audit events, and human-confirmed creation of records/obligations. Frontend document suggestion review UI with AI status. Backend+frontend tests/builds passing. |
| 2026-11-21 | Phase 6 - Household Collaboration | Completed (baseline) | Added invitation/member detail endpoints, invite create/revoke/accept flow, role checks, household-scoped member listing, audit events. Frontend Household page for members, invite, accept token, revoke. Backend+frontend tests/builds passing. |
| 2026-11-21 | Phase 7 - Upgrades | Completed (baseline) | Added Tika MIME detection/text extraction, obligation reschedule endpoint/UI, recurrence rules (DAILY/WEEKLY/MONTHLY/YEARLY) with next-occurrence creation on complete, household owner role update/remove member endpoints/UI, PWA manifest/meta, prod profile, deployment/security/mvp docs. Full backend+frontend verification passing. |
| 2026-11-21 | Pending gap pass | Completed (baseline) | Added in-process reminder delivery scheduler (`@Scheduled`), notification dedupe/read-all, rate limiting filter + security headers, LGPD export/delete endpoints and Settings UI, obligation upgrade/privacy integration tests. Full backend+frontend verification passing. |

## Known Limitations / Blockers

- Docker daemon unavailable in runtime (`/var/run/docker.sock` missing); Postgres container/startup smoke not executed here.
- Password reset/invitation email delivery not configured; tokens are manual/dev baseline.
- Reminder delivery is in-process poll scheduler; production should use durable queue/worker.
- Rate limiting is in-memory single-node baseline; no distributed limiter/AV/dependency scanning CI wired.
- LGPD baseline includes export/delete; correction workflow, retention/backup/restore drills, and formal compliance review pending.
- Required MVP E2E gate and external security audit not executed.
- Workspace is not a git repository; no commits created.

## Product Definition

### Vision
LifeOS is a personal/family life administration platform. It turns documents and personal information into connected records, responsibilities, deadlines, and actions. It is a “memory operacional da vida pessoal”, not a task app, calendar, storage drive, notes app, or chatbot.

### Core principle
Every important piece of information should be organizable, related to other information, and convertible into an action/responsibility when needed.

### Initial market / constraints
- **Initial market**: Brazil; architecture ready for i18n/internationalization.
- **Platform**: Responsive web, PWA-ready.
- **Stack**: React + TypeScript frontend; Java + Spring Boot backend; PostgreSQL.
- **Positioning**: Minimalist, trustworthy, private, clear, no feature bloat.
- **Privacy**: User data belongs to user; no ads; no training on private data; no external sharing without explicit consent/config.

### Domain mental model
| Concept | Meaning | Key rule |
|---|---|---|
| **People** | Users/members in a household space | Records/obligations can be assigned to responsible people |
| **Records** | Anything managed: asset, vehicle, property, service, subscription, contract, other | Must have structured fields; JSONB only for validated type-specific metadata |
| **Documents** | Files/proofs attached to records | Preserve original file; track extraction/review status; can link to multiple records when justified |
| **Obligations** | Something to resolve/pay/check/renew/follow up | Not generic task; has due date, owner, status, recurrence, source, history, notifications |
| **History** | Audit/activity trail | Records who/when/what; no unnecessary sensitive content in logs |

## Key Product Decisions (locked by spec/user)
1. **Inbox is the heart of capture** — user uploads/adds first, organizes after. No module-first thinking required.
2. **Records ≠ Obligations** — notebook is a Record; checking warranty is an Obligation.
3. **AI is human-in-the-loop** — extracts/classifies/suggests; system validates; user confirms before creating/updating obligations or important records.
4. **MVP must be truly usable** — prioritize complete vertical flows over many pretty screens.
5. **Do not start with AI** — build reliable data model and organization flows first; add AI after foundation/records/documents/obligations are stable.
6. **No destructive actions without authorization** — especially delete/share/permission changes/external operations.

## Architecture Decisions

### High-level topology
- **Monorepo**, single deployable backend + single frontend app in early phases.
- **Backend**: modular monolith (Spring Boot), no microservices.
- **Frontend**: SPA (React/Vite) with route-based code splitting, PWA manifest/service worker later.
- **DB**: PostgreSQL as system of record; FTS for search; no Elasticsearch/vector DB in MVP.
- **Files**: storage abstraction; local filesystem in dev, S3-compatible in prod; no binary blobs in relational tables.
- **Async**: DB-backed jobs + scheduled polling in MVP; interface allows future queue (Redis/SQS/etc.) without domain rewrite.

### Suggested repository layout
```text
LifeOS/
├── README.md
├── AGENTS.md                     # agent/workflow instructions; keep concise and factual
├── .gitignore
├── .env.example
├── docker-compose.yml
├── docs/
│   ├── implementation-plan.md    # copy/maintain from this master plan during implementation
│   ├── product/
│   │   ├── vision.md
│   │   ├── personas.md
│   │   ├── mvp-scope.md
│   │   └── user-flows.md
│   ├── architecture/
│   │   ├── overview.md
│   │   ├── backend.md
│   │   ├── frontend.md
│   │   ├── database.md
│   │   ├── security.md
│   │   └── decisions/            # ADRs
│   ├── api/
│   │   └── api-conventions.md
│   └── development/
│       ├── setup.md
│       ├── testing.md
│       └── deployment.md
├── backend/
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/
│   └── src/main/java/com/lifeos/
│       ├── LifeOsApplication.java
│       ├── shared/               # errors, security primitives, pagination, time, ids, audit base
│       ├── identity/             # auth, users, sessions, password reset
│       ├── household/            # households, memberships, invitations, current household
│       ├── record/               # records, relationships, metadata validation
│       ├── document/             # upload, storage, extraction jobs, download, linking
│       ├── obligation/           # obligations, recurrence, completion/cancel/reschedule
│       ├── reminder/             # reminder scheduling/delivery state
│       ├── notification/         # in-app notifications
│       ├── search/               # FTS queries, grouped results
│       ├── activity/             # audit/activity feed
│       └── ai/                   # provider abstraction, extraction/classification - Phase 5
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── db/migration/
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.*
    └── src/
        ├── app/                  # router, providers, layout, guards
        ├── shared/               # ui, api client, hooks, utils, types, schemas
        ├── features/
        │   ├── auth/
        │   ├── household/
        │   ├── dashboard/
        │   ├── inbox/
        │   ├── records/
        │   ├── documents/
        │   ├── obligations/
        │   ├── notifications/
        │   └── search/
        └── test/
```

### Backend module boundaries
- Each module owns its API DTOs, application services, domain rules, persistence entities/repositories, and migrations related to its tables.
- Cross-module access through application services/interfaces in `shared` or explicit module APIs; **no direct repository/entity reach-in across module internals**.
- `shared` contains only truly generic concerns: security context, error model, pagination, clock/timezone helpers, identifiers, storage interface, audit publisher interface, validation utilities.
- Avoid circular dependencies; enforce with package conventions/ArchUnit tests in Phase 1.

### Frontend boundaries
- `shared/ui`: reusable presentational components only.
- `features/*`: route screens, feature hooks, API calls, Zod schemas, feature types.
- No business rules hidden in components; server is source of truth for authorization and state transitions.
- TanStack Query for server state; React Hook Form + Zod for forms; minimal local UI state.

## Data Model Plan

### Core tables (MVP)
| Table | Purpose | Key fields / constraints |
|---|---|---|
| `users` | Identity | `id uuid pk`, `email unique citext/lower`, `password_hash`, `display_name`, `locale`, `timezone`, timestamps |
| `households` | Personal/family space | `id`, `name`, `owner_user_id fk`, timestamps |
| `household_memberships` | User role in household | `id`, `household_id`, `user_id`, `role`, `joined_at`, unique(household_id,user_id), indexes |
| `household_invitations` | Email invite | `id`, `household_id`, `invited_email`, `token_hash`, `role`, `expires_at`, `accepted_at`, `revoked_at`, `created_by`, unique active token behavior |
| `records` | Managed items | `id`, `household_id`, `type`, `title`, `description`, `category`, `status`, `responsible_member_id`, `acquired_at`, `started_at`, `archived_at`, `amount_minor`, `currency`, `metadata jsonb`, timestamps; FK + household indexes |
| `record_relationships` | Record-to-record links | `id`, `household_id`, `from_record_id`, `to_record_id`, `relationship_type`, timestamps; prevent self-link unless justified |
| `documents` | File metadata + extraction state | `id`, `household_id`, `uploaded_by_member_id`, `original_filename`, `storage_key`, `mime_type`, `size_bytes`, `checksum_sha256`, `extraction_status`, `extracted_text`, `extraction_error`, timestamps |
| `record_documents` | Record-document association | `record_id`, `document_id`, `household_id`, `relationship_type`, timestamps; unique(record_id,document_id,relationship_type) |
| `obligations` | Responsibilities/deadlines | `id`, `household_id`, `record_id nullable`, `title`, `description`, `responsible_member_id`, `due_at timestamptz`, `priority`, `status`, `recurrence_rule jsonb/structured`, `source_type`, `source_document_id nullable`, timestamps |
| `reminders` | Scheduled reminder instances | `id`, `household_id`, `obligation_id`, `scheduled_at`, `channel`, `status`, `delivered_at`, `dedupe_key unique` |
| `notifications` | In-app user notifications | `id`, `household_id`, `user_id`, `type`, `title`, `body`, `read_at`, `related_entity_type/id`, `created_at` |
| `audit_events` | History/audit | `id`, `household_id`, `actor_user_id`, `entity_type`, `entity_id`, `action`, `occurred_at`, `metadata jsonb` sanitized |
| `extraction_jobs` | Async doc processing | `id`, `household_id`, `document_id`, `status`, `provider`, `attempt_count`, `result jsonb`, `error_code`, `created_at`, `started_at`, `completed_at` |

### Modeling rules
- **All tenant-owned tables include `household_id`** and are queried by it. Composite indexes start with `household_id` where appropriate.
- **No direct JPA entity exposure**. DTOs map explicitly.
- **JSONB limited** to validated extensible metadata (`records.metadata`, recurrence definition if needed, extraction result, sanitized audit metadata). Important query/filter fields get real columns.
- **Soft delete/archive only where user-facing recovery/audit requires it**; otherwise explicit delete with audit event and confirmation. Documents/records use `archived_at` or status where appropriate; destructive delete path documented.
- **Enum strategy**: DB check constraints or varchar + application enum validation; migrations define allowed values carefully.
- **Time**: store instants in `timestamptz`; store user timezone in profile; render in user timezone; recurrence calculations use user/household timezone explicitly.

## Security & Privacy Plan

### Auth/session
- Spring Security with server-side session (recommended for web MVP): session cookie `HttpOnly`, `Secure` in prod, `SameSite=Lax` (or CSRF-compatible strategy), CSRF protection enabled for browser flows.
- Password hashing: Argon2id (Spring Security encoder) or BCrypt only if Argon2 unavailable; prefer Argon2id.
- No sensitive tokens in localStorage. Invite/reset tokens stored hashed.
- Password reset tokens: single-use, short expiry, invalidated on use/password change.

### Authorization / multi-tenancy
- Every request resolves authenticated user and active household membership.
- Repository/service queries always scoped by `household_id` + permission checks.
- Role matrix enforced server-side:
  - OWNER: full control, members, delete household.
  - ADMIN: manage records/docs/obligations/members per policy.
  - MEMBER: view/manage allowed records/docs/obligations.
  - VIEWER: read-only.
- IDOR tests mandatory for all entity endpoints.

### Documents/files
- Validate real MIME via content detection (Tika/Apache detector), extension allowlist, max size configurable.
- Safe internal storage key (UUID/path); keep original filename as metadata, sanitize on display/download headers.
- Store outside public web root; downloads via authorized controller/short-lived signed URL abstraction.
- No executable uploads. Antivirus hook/interface; ClamAV optional in dev/prod, but interface and validation required.

### Privacy/LGPD
- Data export endpoint/job (later phase) produces user/household data package; document scope clear.
- Account deletion flow with explicit confirmation; document retention/backup caveats in policy docs.
- Audit logs exclude document contents, secrets, raw tokens, full prompt payloads with sensitive data.
- AI external processing disabled by default unless configured and consented; manual flow always works.

## API & Contract Conventions

- REST, versioned: `/api/v1`.
- JSON camelCase; timestamps ISO-8601; UUID ids.
- DTOs only; no entity leakage.
- Consistent error envelope: `code`, `message`, `details[]`, `traceId`, `timestamp`.
- Pagination: cursor or page+size consistently (recommend page+size for MVP simplicity) with `totalElements/totalPages` where needed.
- Filtering/sorting explicit query params; validate allowed sort fields.
- OpenAPI via springdoc; keep spec generated and checked in CI or build artifact.
- Idempotency for jobs/reminder generation; mutation endpoints validate state transitions.

### Initial endpoint map (phase-gated)
| Phase | Endpoints |
|---|---|
| 1 | auth register/login/logout/me, password reset request/confirm, current household, members/invitations basic, health |
| 2 | records CRUD/list/detail/archive, record relationships, activity/history read |
| 3 | documents upload/list/detail/download/delete, record-document link/unlink, inbox states |
| 4 | obligations CRUD/complete/cancel/reschedule, reminders internal, notifications list/read, dashboard, search |
| 5 | document extract/review/confirm, AI suggestions, assistant query (read-only, grounded) |

## UX/UI Plan

### Visual direction
- Minimal, calm, professional, light theme default; dark theme later.
- Strong typography hierarchy, generous spacing, subtle borders/cards, one accent color.
- No dashboard chart clutter; actionable summaries first.
- Clear empty/loading/error/success states.

### Navigation (MVP)
- Desktop: left sidebar + top context bar/search/quick capture.
- Mobile: bottom nav for primary areas + FAB/quick action for capture; responsive detail layouts.
- Primary areas: Today, Inbox, Records, Documents, Obligations, Household, Settings.

### Key screens by phase
- **Phase 1**: Login/register/reset, onboarding/create personal space, app shell, settings/profile, household members/invite basic.
- **Phase 2**: Records list/filter/search, record create/edit, record detail with tabs/sections (info, docs placeholder, obligations placeholder, history).
- **Phase 3**: Inbox/upload/review, document list/detail/preview/download, link document to record.
- **Phase 4**: Obligations list/calendar-lite agenda, create/edit/complete, Today dashboard, notifications.
- **Phase 5**: Extraction review UI with evidence/confidence, confirm-to-create record/obligation flow.

### Accessibility/responsiveness
- Keyboard navigable, visible focus, semantic landmarks, labels/errors tied to inputs, contrast compliant.
- Mobile-first layouts; no desktop-shrunk tables as only view.
- PWA: manifest, icons, installability, offline-safe shell later; no fake offline data claims.

## Observability, Quality, and DevEx

### Local dev
- Docker Compose: PostgreSQL, MinIO optional, backend, frontend; `.env.example` with safe defaults.
- Backend: Maven wrapper, Flyway migrations, Spring profiles (`local`, `test`, `prod`).
- Frontend: Vite dev server, typed API client, env var for API base URL.
- Seed data only in `local/dev` profile; clearly marked fake data; never in prod.

### Testing strategy
- **Backend**: JUnit 5, Spring Boot Test, Testcontainers (Postgres), MockMvc/WebTestClient, authorization/security tests, time-controlled tests for recurrence/timezones.
- **Frontend**: Vitest + React Testing Library for units/integration; Playwright for critical E2E.
- **Security tests**: IDOR/cross-household, role checks, CSRF/session, file MIME spoofing, upload size, invite expiry/reuse, log redaction.
- **Required E2E before MVP claim**: create user -> personal space -> upload doc -> process/review -> create record -> link doc -> create obligation -> notification -> dashboard -> search -> history.

### CI/quality gates (implement early)
- Backend: format/checkstyle/spotless (choose one), unit+integration tests, Flyway validation.
- Frontend: typecheck, lint, unit tests, build.
- Secret scanning/gitignore checks; no `.env` committed.
- Docs updated with every significant decision (ADR).

## Phased Implementation Roadmap

### Phase 0 — Discovery & repo bootstrap (first execution session)
1. Confirm workspace remains empty; create repo skeleton and Git repo only if user approves git init; otherwise keep files ready.
2. Create `README.md`, `AGENTS.md`, `.gitignore`, `.env.example`, `docs/implementation-plan.md`, initial ADRs.
3. Inspect toolchain versions and record in docs; decide Java LTS/Spring Boot stable versions available locally.
4. **Exit criteria**: documented baseline, no assumptions hidden.

### Phase 1 — Foundation (first code milestone, recommended now)
1. Scaffold backend Maven/Spring Boot modular monolith + frontend Vite React TS Tailwind shadcn.
2. Docker Compose for Postgres (+ MinIO optional), app health checks, local profiles.
3. Identity: register/login/logout/me, password reset flow, Argon2id, secure session+CSRF.
4. Household: auto-create personal household on signup, current household context, membership roles, invitation model/API skeleton with secure tokens.
5. App shell: auth guards, responsive navigation, Today/Inbox/Records/Documents/Obligations/Household/Settings placeholders, quick-capture entry point.
6. Audit/security primitives: request/user/household context, error envelope, OpenAPI, base audit event publisher.
7. Tests: auth, household isolation smoke, CSRF/session, basic frontend auth flow.
8. **Exit criteria**: user can sign up, log in, land in personal space, see usable shell; local run documented and passing.

### Phase 2 — Records (vertical slice) — baseline completed 2026-11-21
1. [x] Record CRUD/list/detail/archive, type/category/status/responsible/structured fields.
2. [x] Record relationships and history/audit feed.
3. [x] Search foundation for records (case-insensitive title/description/category query; Postgres tsvector/GIN FTS still pending).
4. [x] UI: records list/filter/search, create, detail, edit, archive, history, basic relationship add.
5. **Exit criteria**: baseline met for authenticated household-scoped record management; full Postgres FTS tuning and richer UX polish remain.

### Phase 3 — Documents & Inbox — baseline completed 2026-11-21
1. [x] Storage abstraction + local impl, secure upload/download, basic MIME/size validation, safe storage keys.
2. [x] Document metadata, inbox states, record-document linking, authorized access.
3. [x] Extraction job/status plumbing (basic text extraction for TXT; PDF/image status tracked, no AI provider).
4. [x] Inbox/Documents UI: upload, list/filter, extracted-text preview, download, link to record, delete confirmation.
5. **Exit criteria**: baseline met for authenticated household-scoped upload/list/link/download/delete; Tika/PDFBox async extraction, richer review/reprocess UI, and stronger MIME sniffing remain.

### Phase 4 — Obligations, Reminders, Dashboard, Search — baseline completed 2026-11-21
1. [x] Obligation CRUD/complete/cancel, responsible, priority, record link, source fields baseline.
2. [~] Reminder generation + dedupe key on create; full recurrence engine/DST/idempotent scheduler pending.
3. [x] In-app notifications on obligation creation; email/push adapters not implemented.
4. [x] Today dashboard: overdue, due soon, recent records, notifications.
5. [x] Global search across records/docs/obligations (grouped response; basic query matching, not full FTS tuning).
6. **Exit criteria**: baseline usable without AI; full recurrence/scheduler, reschedule, richer dashboard states, and E2E gate remain.

### Phase 5 — AI-assisted capture — baseline completed 2026-11-21
1. [x] Config abstraction (`lifeos.ai.enabled/provider`) default disabled; explicit 412 when disabled.
2. [~] Suggestion pipeline implemented as safe local baseline (no external model yet); structured payloads stored/audited. External provider/schema validation pending.
3. [x] Review UI with evidence/confidence and confirm/reject; field-level editing before confirm pending.
4. [x] Confirmed suggestions create records/obligations; no auto-confirm. No external hallucinated dates (baseline uses deterministic due date).
5. [x] Audit events recorded for suggestion create/confirm/reject; prompt redaction N/A until external provider.
6. **Exit criteria**: human-in-the-loop suggestion flow works when enabled; manual flow works when disabled.

### Phase 6 — Family/shared household — baseline completed 2026-11-21
1. [x] Invitation create/accept/revoke flow, member listing; role-change UI/API pending.
2. [~] Per-role server enforcement for invite management and existing module writes; full per-role UI gating/activity views pending.
3. [ ] Shared responsibility views and filters.
4. **Exit criteria**: baseline invite/collaboration flow works with email-match and household scoping; role management and richer shared views remain.

### Phase 7 — Polish & production readiness — baseline upgrades completed 2026-11-21
1. [x] PWA manifest/theme metadata, responsive UI baseline, route-level polish.
2. [~] Deployment/security/MVP scope docs added; export/delete/correction/consent flows and backup/restore drill pending.
3. [~] Actuator health/info exposed; structured audit/error trace IDs present; metrics/job monitoring pending.
4. [~] Tika MIME detection/text extraction added; rate limits, AV integration, dependency/container scanning, secret-manager production wiring pending.
5. **Exit criteria**: not fully MVP-complete; major gaps closed, known limitations documented, verification green.

## Risks and Mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Multi-tenancy/IDOR bugs | Critical data leak | `household_id` everywhere, centralized authz checks, negative tests per endpoint |
| AI hallucinated dates/fields | Wrong obligations/data | Human review required, evidence required, schema validation, no invented due dates |
| Recurrence/timezone bugs | Missed/duplicate reminders | Store UTC instants, explicit user TZ, dedupe keys, DST tests, idempotent jobs |
| Document handling vulnerabilities | Malware/content spoofing/data exposure | MIME sniffing, size limits, safe storage keys, authz download, no public paths, AV interface |
| Scope creep into disconnected modules | Unusable/incoherent product | Vertical slices, Inbox/Records/Obligations core, defer non-MVP features |
| Overuse of JSONB/unstructured metadata | Hard queries/inconsistent data | Structured columns for core fields; JSONB only validated type-specific extras |
| Empty repo/no CI/tooling drift | Slow/fragile delivery | Phase 0 bootstrap with scripts, Docker, tests, docs, ADRs before feature expansion |

## Validation / Definition of Done

### Feature DoD
- Implemented, integrated, validated, authorized, error-handled, tested, locally runnable, documented, no known critical issues.

### MVP DoD (before claiming MVP)
- Auth + personal household + records + documents/inbox + obligations + Today dashboard + global search + history all work end-to-end.
- Priority E2E passes: create user -> create personal space -> upload test doc -> process doc -> review info -> create record -> link doc -> create obligation -> create notification -> see dashboard -> search record -> consult history.
- Security checks pass for cross-household access, unauthorized download, invite misuse, CSRF/session, MIME spoofing/oversize upload.
- Docs current: setup, testing, architecture, ADRs, known limitations, implementation status.

## Assumptions / Defaults (conservative)
- **Java 21 LTS + latest stable Spring Boot 3.x** unless local environment requires adjustment; verify during Phase 0.
- **Session-cookie auth** for browser-first web app; CSRF enabled. JWT only if later non-browser clients require it.
- **Page-based pagination** for MVP simplicity.
- **Postgres FTS + btree/gin indexes**; no external search engine.
- **Local storage in dev, S3-compatible in prod** behind `StorageService`.
- **In-app notifications first**; email/push adapters later.
- **No git init/commits** will be performed by implementation agent without explicit user request; preserve/inspect existing git state if present.
- **This plan is the master planning artifact**; implementation must also maintain `docs/implementation-plan.md` in-repo as living status/plan per spec.

## Implementation Agent Handoff Checklist
1. Read this plan and inspect repo before changes.
2. Execute Phase 0 bootstrap; record actual tool versions and decisions.
3. Implement Phase 1 as a complete vertical foundation; do not skip tests/docs.
4. After each phase: run build/tests/lint/typecheck, update docs and status, report real results/blockers.
5. Keep AI disabled/manual-safe until Phase 5 and only after core flows are reliable.