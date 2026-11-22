# ADR 0001 — Initial Foundation

- **Date**: 2026-11-21
- **Status**: Accepted

## Context

LifeOS starts in an empty workspace and must prioritize trustworthy, connected personal data flows.

## Decisions

- Use a monorepo with `backend/` and `frontend/`.
- Backend: Java 21, Spring Boot modular monolith, Maven, PostgreSQL, Flyway.
- Frontend: React + TypeScript + Vite + Tailwind.
- Auth: server-side session + CSRF for browser-first MVP.
- AI: disabled/manual-safe until Phase 5.
- No microservices, no Elasticsearch/vector DB, no entity exposure in MVP.

## Consequences

- Faster, coherent vertical slices with clear module boundaries.
- Requires discipline around household scoping, DTO mapping, and authz tests.
