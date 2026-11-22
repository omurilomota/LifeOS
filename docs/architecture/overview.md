# Architecture Overview

## Topology

LifeOS starts as a modular monolith:

- **Frontend**: React + TypeScript SPA (Vite), route-based feature modules.
- **Backend**: Spring Boot modular monolith with clear package/module boundaries.
- **Database**: PostgreSQL as system of record.
- **Files**: storage abstraction; local filesystem in dev, S3-compatible later.

## Modules

Backend modules under `com.lifeos`:

- `shared` - errors, security context, pagination, time, audit, storage interfaces
- `identity` - users, auth, sessions, password reset
- `household` - households, memberships, invitations
- `record` - records and relationships
- `document` - documents, upload/download, extraction jobs
- `obligation` - obligations, recurrence, completion/cancel/reschedule
- `reminder` - reminder scheduling/delivery state
- `notification` - in-app notifications
- `search` - FTS/search queries
- `activity` - audit/history
- `ai` - provider abstraction and human-in-the-loop extraction (Phase 5)

## Key constraints

- All tenant-owned data includes `household_id`.
- No cross-module repository reach-in.
- REST DTOs only; never expose JPA entities.
- AI disabled/manual-safe until Phase 5 and configured.
