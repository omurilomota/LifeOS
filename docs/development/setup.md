# Development Setup

## Prerequisites

- Java 21+
- Maven 3.9+
- Node 24+ / npm 12+
- Docker + Docker Compose

## Environment

```bash
cp .env.example .env
```

Review values before sharing or deploying. Do not commit `.env`.

## Start infrastructure

```bash
docker compose up -d postgres
```

## Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Useful endpoints:

- Health: `http://localhost:8080/api/v1/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Frontend

```bash
cd frontend
npm install
npm run dev
```

App: `http://localhost:5173`

For a deployed frontend against a non-local backend, set `VITE_API_BASE_URL` (e.g. `https://your-api.example.com/api/v1`) before build/deploy.

## Demo mode

For quick smoke testing, the UI exposes **"Entrar sem conta (demo)"**. It creates an isolated demo user/household through `POST /api/v1/auth/demo`. Treat demo accounts as disposable test data; do not use for real personal information in public environments.

## Tests

```bash
cd backend && mvn test
cd frontend && npm run typecheck && npm run lint && npm run test && npm run build
```

## Troubleshooting

- **Docker socket missing**: start Docker Desktop/daemon; Postgres smoke tests require it.
- **CORS/CSRF in cross-origin deploy**: set backend `CORS_ALLOWED_ORIGINS`, `SESSION_COOKIE_SECURE=true`, `SESSION_COOKIE_SAMESITE=NONE`; frontend must send credentials and `X-XSRF-TOKEN`.
- **401 on `/auth/me` before login**: expected when there is no session.
