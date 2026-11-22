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
- Critical flow E2E smoke with Playwright (MVP gate) — see below.

Run:

```bash
cd frontend
npm run typecheck
npm run lint
npm run test
npm run build
```

## E2E smoke (MVP gate) — Playwright

The required core flow is automated in `frontend/e2e/core-flow.spec.ts`. It uses **demo mode** (`Entrar sem conta (demo)`) so every run gets an isolated user/household, and covers:

1. Enter the app (demo mode)
2. Create a record and open its detail/history
3. Upload a `.txt` document and wait for Tika extraction (`READY`)
4. Link the document to the created record
5. Create an overdue obligation (priority `HIGH`, no recurrence)
6. Verify dashboard: overdue obligation, recent record and generated notification
7. Global search finds the created record
8. Complete the obligation and confirm `COMPLETED`

`playwright.config.ts` starts the backend (`mvn spring-boot:run -Dspring-boot.run.profiles=local`) and the Vite dev server automatically, and reuses them when already running locally. It requires a reachable PostgreSQL on `localhost:5432` with the `.env.example` defaults; start it with:

```bash
docker compose up -d postgres
cd frontend && npm run test:e2e
```

Use `npx playwright install chromium` once after installing dependencies. Locally, reports/traces are written to `frontend/playwright-report/` and `frontend/test-results/` (gitignored). This gate is also executed in CI (`.github/workflows/ci.yml`, job `e2e`) with a PostgreSQL service container.

Track this in [issue #1](https://github.com/omurilomota/LifeOS/issues/1).

## Manual smoke (current)

- Start Postgres, backend, frontend.
- Register or use **"Entrar sem conta (demo)"** for quick local/public smoke.
- Verify `/api/v1/health`, auth session, dashboard, record/document/obligation flows.
- Confirm CORS/CSRF work when frontend and backend are on different origins.

## CI

GitHub Actions runs backend tests, frontend typecheck/lint/test/build and the E2E smoke gate on pushes and PRs to `main`/`develop` (`.github/workflows/ci.yml`).
