# Testing

## Backend

```bash
cd backend
mvn test
```

Planned/implemented test layers:

- Unit tests (JUnit 5)
- Spring Boot integration tests
- Security/authorization tests (IDOR, household isolation, CSRF/session)
- Time-controlled tests for scheduling/recurrence (Phase 4)

## Frontend

```bash
cd frontend
npm run test
npm run typecheck
npm run lint
```

## E2E (MVP gate)

Before claiming MVP, the priority flow must pass:

1. Create user
2. Create personal space
3. Upload test document
4. Process/review document
5. Create record
6. Link document
7. Create obligation
8. Create/receive notification
9. View dashboard
10. Search record
11. Consult history
