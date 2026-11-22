# Development Setup

## Toolchain (verified 2026-11-21)

- Java: OpenJDK 21.0.13.1
- javac: 21.0.13.1
- Maven: 3.9.12
- Node: v24.20.0
- npm: 12.2.2
- Docker: 29.10.0
- Docker Compose: v5.8.0

## Local run

1. `cp .env.example .env`
2. `docker compose up -d postgres`
3. Backend: `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local`
4. Frontend: `cd frontend && npm install && npm run dev`

## Notes

- Do not commit `.env` or secrets.
- PostgreSQL is required for backend runtime.
- AI integrations are disabled by default.
