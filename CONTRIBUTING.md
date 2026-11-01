# Conventions

## Tests
- **Tag every test** with a layer (`ui`, `api`, `db`, `integration`) and a level (`smoke` = critical, fast, stable; `regression` = everything else).
- Name tests by behavior: `@DisplayName("Invalid credentials show an error")`. Class names end with `Test`.
- **No `Thread.sleep`.** Playwright auto-waits; for async systems use `WaitUtils.poll()` or `elastic.waitForHits(...)`.
- Tests must be **independent and parallel-safe**: create your own data (`TestDataFactory`), use unique names (UUIDs), clean up in `finally`/`@AfterEach`, never share mutable static state.
- One reason to fail per test. Use `assertSoftly` when you need to check several fields of one response.
- Prefer a mocked API (`UserApiMockedTest` pattern) for logic checks; use the real service for a small set of contract/smoke checks.

## Layers
- `pages/`: locators and user actions only. Return `this` or the next page. **No assertions.**
- `clients/`: one method per endpoint, return `Response`. **No assertions.** Annotate with `@Step`.
- `repositories/`: SQL only.
- New infrastructure or clients are created through `ClientFactory` / `PageFactory`.
- Test data models are records; give complex ones a builder (see `LogEntry`).

## Locators (UI)
Prefer, in order: `getByRole` / `getByLabel` / `getByTestId`, then stable CSS ids, and avoid XPath chains and text that changes.

## Git
- Branch from `main`, small PRs, fill in the PR checklist. Commit messages in imperative mood ("Add login negative test").
- Never commit secrets, `logs/` or `target/`.
