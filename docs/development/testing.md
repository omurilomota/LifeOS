# Testing

## Backend (JUnit 5 + Spring Boot Test)

- **Unit tests**: services/domain logic, DTO validation rules, permission decisions.
- **Integration tests**: MockMvc/WebTestClient + Testcontainers PostgreSQL for auth, household scoping, CRUD, upload/download authorization.
- **Security tests**: IDOR/cross-household access, CSRF/session behavior, file upload validation.
- **Migrations**: Flyway runs in integration tests against PostgreSQL.

Run:

```bash
cd backend
mvn test
```

## Frontend (Vitest + Testing Library)

- Component/form validation tests.
- API client/error handling tests.
- Critical flow smoke/E2E later (Playwright) for MVP gate.

Run:

```bash
cd frontend
npm run typecheck
npm run lint
npm run test
npm run build
```

## Required MVP E2E flow

Before claiming MVP complete, automate:

1. Register/login
2. Create record
3. Upload document
4. Link document to record
5. Create obligation
6. See dashboard/today
7. Search finds record/document
8. Complete obligation

Track this in [issue #1](https://github.com/omurilomota/LifeOS/issues/1).

## Manual smoke (current)

- Start Postgres, backend, frontend.
- Register or use **"Entrar sem conta (demo)"** for quick local/public smoke.
- Verify `/api/v1/health`, auth session, dashboard, record/document/obligation flows.
- Confirm CORS/CSRF work when frontend and backend are on different origins.

## CI

GitHub Actions runs backend tests and frontend typecheck/lint/test/build on pushes and PRs to `main`/`develop` (`.github/workflows/ci.yml`).
