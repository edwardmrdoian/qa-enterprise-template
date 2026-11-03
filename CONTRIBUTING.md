# Contributing

Use JDK 25 and the committed Maven Wrapper. Work in a small feature branch from the
repository's default branch (`master` in this repository).

## Before opening a PR

```bash
./mvnw spotless:apply
./mvnw clean verify
```

Install Chromium first as described in README. Changes to initialization also require
`pwsh -File scripts/verify-template.ps1`. Changes to messaging/search need the optional
Docker integration suite where Docker is available.

## Tests and boundaries

- `framework` checks must be deterministic and self-contained after dependencies/browser
  installation. They may use localhost mocks, local UI fixtures and isolated H2 pools.
- Tag application tests `project`, a layer, and optionally `smoke`/`regression`.
  Tag teaching examples `example`; public-service examples also need `external`.
- Use `@DisplayName` or behavioral method names. Test class names end in `Test`.
- Use Playwright's retrying assertions for UI state. No `Thread.sleep`.
- Poll only for expected eventual state. Do not swallow arbitrary exceptions or automatically
  retry assertions in required CI. Selected suites must not quietly skip prerequisites.
- Each test owns its data and cleanup. Use unique IDs, `finally`, try-with-resources or fixtures.
  Never share mutable WireMock mappings across parallel test methods.
- Inject `Page` into page/component objects and base URLs into API clients where practical.
  Factories are convenience helpers, not mandatory abstraction layers.
- Tests contain assertions; pages contain locators/actions; API clients contain endpoint
  calls; repositories contain parameterized SQL. Keep business-specific models outside core helpers.
- Reuse a helper only when it simplifies real test code. Avoid giant base classes and
  generic wrappers around every library method.

## Secrets and diagnostics

Never commit credentials, `.env`, `target/` or `logs/`. Rotate any exposed credential.
Default HTTP reporting excludes headers, query values and bodies; redact any custom
attachments. Do not include passwords in Allure `@Step` parameters.
Screenshots/traces can contain sensitive data; treat CI artifacts accordingly.

## Dependency updates

Keep the Playwright Maven version and Docker image tag aligned. Review major dependency
upgrades instead of merging blindly. A version bump must preserve Java 25 compatibility,
the default suite, Allure generation and the Docker integration suite where applicable.
