# API Conventions

- Base path: `/api/v1`
- JSON camelCase.
- UUID identifiers.
- ISO-8601 timestamps.
- DTOs only; no JPA entity exposure.
- Errors use consistent envelope:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [],
  "traceId": "...",
  "timestamp": "2026-11-21T00:00:00Z"
}
```

- Auth is server-side session cookie + CSRF for browser flows.
- All tenant-owned endpoints resolve authenticated user + active household and enforce authorization.
