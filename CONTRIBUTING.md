# Contributing to LifeOS

Thank you for your interest in contributing.

## Before contributing

- Read [`README.md`](README.md), [`AGENTS.md`](AGENTS.md), and [`docs/implementation-plan.md`](docs/implementation-plan.md).
- Check [open issues](https://github.com/omurilomota/LifeOS/issues), especially labels `good first issue` and `help wanted`.
- For large changes, open an issue before coding so scope and approach can be aligned.

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

5. Open a pull request to `develop` with a clear description, test evidence, and security/privacy impact.

## Pull request expectations

- Keep PRs focused; avoid unrelated refactors.
- Add/update tests for behavior and authorization changes.
- Update docs when API behavior, config, or workflows change.
- Include screenshots/GIFs for visible UI changes.
- Do not include secrets, personal data, or production credentials.

## Code and security expectations

- Preserve household scoping and server-side authorization checks.
- Do not expose JPA entities directly in APIs.
- Add IDOR/cross-household tests for tenant-owned endpoints.
- Keep AI features disabled/manual-safe by default; suggestions require human review.
- Handle destructive actions with explicit confirmation and authorization.

## Good first contributions

- Documentation improvements and examples.
- Test coverage (unit/integration/E2E).
- Accessibility fixes.
- Small UX refinements with screenshots.
- CI/security tooling improvements.

## License

By contributing, you agree that your contributions will be licensed under the repository's [AGPL-3.0](LICENSE) license.
