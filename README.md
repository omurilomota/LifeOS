# LifeOS

LifeOS is a personal/family life administration platform: a memory operacional da vida pessoal. It turns documents and personal information into connected records, responsibilities, deadlines, and actions.

## Status

Early foundation (Phase 0/1 bootstrap). The workspace is being scaffolded according to `docs/implementation-plan.md`.

## Stack

- **Frontend**: React + TypeScript + Vite + Tailwind CSS
- **Backend**: Java 21 + Spring Boot modular monolith + Maven
- **Database**: PostgreSQL
- **Local infra**: Docker Compose

## Repository layout

```text
LifeOS/
├── backend/                  # Spring Boot API
├── frontend/                 # React SPA
├── docs/                     # Product, architecture, API, development docs
├── docker-compose.yml        # Local PostgreSQL and optional services
└── .env.example              # Safe local defaults
```

## Quick start (local)

### Prerequisites

- Java 21+
- Node 24+ / npm 12+ (or compatible recent LTS)
- Maven 3.9+
- Docker + Docker Compose

### 1) Configure env

```bash
cp .env.example .env
```

### 2) Start PostgreSQL

```bash
docker compose up -d postgres
```

### 3) Run backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Backend health: `http://localhost:8080/api/v1/health`

### 4) Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## Testing

```bash
cd backend && mvn test
cd frontend && npm run test
cd frontend && npm run typecheck
```

## Documentation

- Master plan: `docs/implementation-plan.md`
- Architecture overview: `docs/architecture/overview.md`
- Setup: `docs/development/setup.md`
- Testing: `docs/development/testing.md`
- API conventions: `docs/api/api-conventions.md`

## Contributing

Contributions are welcome. Read [`CONTRIBUTING.md`](CONTRIBUTING.md), [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md), and [`SECURITY.md`](SECURITY.md) before opening issues or pull requests.

## License

[GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0). This license requires that modified versions made available over a network provide corresponding source to users of that service, helping protect the project from proprietary closed-source reuse.

## Principles

- User data belongs to the user; no ads, no private-data training.
- AI is human-in-the-loop and disabled/manual-safe until configured.
- No destructive actions without authorization.
- Every important piece of information should be organizable, related, and convertible into action.
