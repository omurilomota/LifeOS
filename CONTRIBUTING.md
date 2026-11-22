# Contributing to LifeOS

Thank you for your interest in contributing.

## Before contributing

- Read `README.md`, `AGENTS.md`, and `docs/implementation-plan.md`.
- Open an issue before large changes so scope and approach can be aligned.
- Keep changes focused and avoid unrelated refactors.

## Development workflow

1. Fork the repository and create a branch from `develop`.
2. Use Gitflow-style branches:
   - `feature/<short-name>` for features
   - `fix/<short-name>` for bug fixes
   - `docs/<short-name>` for documentation
   - `chore/<short-name>` for maintenance
3. Make small, reviewable commits. Gitmoji-style commit messages are welcome.
4. Run relevant checks before opening a PR:

```bash
cd backend && mvn test
cd frontend && npm run typecheck && npm run lint && npm run test && npm run build
```

5. Open a pull request to `develop` with a clear description, test evidence, and any security/privacy impact.

## Code and security expectations

- Do not commit secrets, tokens, personal data, or production credentials.
- Preserve household scoping and server-side authorization checks.
- Do not expose JPA entities directly in APIs.
- Add or update tests for behavior and authorization changes.
- Document architectural or product decisions in `docs/`.

## License

By contributing, you agree that your contributions will be licensed under the repository's AGPL-3.0 license.
