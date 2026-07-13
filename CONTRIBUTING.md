# Contributing

This project is under active development. Keep changes focused, reviewable, and aligned with the task specifications in `docs/tasks/tasks`.

## Workflow

- Use feature branches for non-trivial changes.
- Keep commits focused on one behavior, fix, or documentation update.
- Do not mix unrelated refactors with feature work.
- Preserve existing formatting and architecture boundaries.
- Update documentation when behavior, configuration, or operator workflows change.
- Run relevant focused tests before committing.
- Run the full Gradle check before opening a review when feasible.
- Never commit secrets, credentials, receipt files, private keys, real bank-card information, or local `.env` files.

## Commit Messages

Use concise conventional commit style:

```text
type(scope): description
```

Recommended types:

- `chore`
- `feat`
- `fix`
- `test`
- `docs`
- `refactor`
- `build`
- `ci`
- `perf`

Examples:

```text
chore(repo): initialize repository
feat(user): add user registration
fix(payment): prevent duplicate approval
test(xui): add provisioning concurrency tests
docs(payment): document Zarinpal callback flow
```

Do not enforce a complex release process yet.
