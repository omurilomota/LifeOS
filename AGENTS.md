# AGENTS.md — LifeOS

## Purpose

LifeOS is a personal/family life administration platform. Prioritize trustworthy, private, connected data flows over feature bloat.

## Source of truth

- Read `docs/implementation-plan.md` before substantial work.
- Update docs when decisions or implementation status change.
- Keep this file concise and factual.

## Architecture guardrails

- **Monorepo**: `backend/` (Spring Boot modular monolith) + `frontend/` (React TS SPA).
- **No microservices** in MVP.
- **PostgreSQL** is system of record; all tenant data scoped by `household_id`.
- **No entity exposure**: REST DTOs only.
- **No sensitive data in logs**: no secrets, tokens, document contents, raw sensitive prompts.
- **AI is human-in-the-loop** and disabled/manual-safe until Phase 5 and configured.
- **No destructive actions without authorization/confirmation** (delete/share/permission/external).

## Module boundaries

- Backend modules: `shared`, `identity`, `household`, `record`, `document`, `obligation`, `reminder`, `notification`, `search`, `activity`, `ai`.
- Cross-module access through explicit services/interfaces; no direct repository reach-in across module internals.
- Frontend feature boundaries under `src/features/*`; shared UI/utilities under `src/shared/*`.

## Security

- Server-side auth/session with CSRF for browser flows.
- Password hashing Argon2id preferred.
- Every request resolves user + active household membership; enforce server-side authorization.
- IDOR/cross-household tests required for entity endpoints.
- File uploads: validate real MIME, size, safe storage keys; authorize downloads.

## Commands

```bash
# Infra
docker compose up -d postgres

# Backend
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd backend && mvn test

# Frontend
cd frontend && npm install
cd frontend && npm run dev
cd frontend && npm run test
cd frontend && npm run typecheck
cd frontend && npm run lint
```

## Definition of done

- Implemented, integrated, validated, authorized, error-handled, tested, locally runnable, documented.
- Do not claim MVP until the priority E2E flow passes.
