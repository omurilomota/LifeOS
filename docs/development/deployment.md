# Deployment (Baseline)

## Target

- Single backend deployable (Spring Boot jar/container)
- Single frontend static build (Vite dist) served by CDN/web server
- PostgreSQL managed database
- Object storage (S3-compatible) for documents in production

## Backend

- Build: `cd backend && mvn -DskipTests package`
- Run: `java -jar target/lifeos-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod`
- Required env: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `SESSION_COOKIE_SECURE=true`
- Optional: `FILE_STORAGE_ROOT` or future S3 config, `AI_ENABLED`, `AI_PROVIDER`

## Frontend

- Build: `cd frontend && npm ci && npm run build`
- Serve `frontend/dist` behind HTTPS.
- Configure `VITE_API_BASE_URL` at build time.

## Security checklist

- HTTPS only; secure cookies.
- Secrets via secret manager/env, never committed.
- DB backups and restore drill.
- Restrict CORS origins.
- Run dependency and container scans before production.
