# QA Enterprise Template

Clone it, rename it, start writing tests. One framework for **UI (Playwright)**, **API (REST Assured + WireMock)**, **DB (JDBI + HikariCP)**, **RabbitMQ** and **Elasticsearch/ELK** tests, with parallel execution, Allure reports, failure traces and CI included.

**Stack:** Java 21 · Maven · JUnit 5 · Playwright · REST Assured · WireMock · JDBI/HikariCP (H2 or PostgreSQL) · RabbitMQ client · Awaitility · AssertJ · Instancio + Datafaker · Owner (config) · Allure · Logback · GitHub Actions

---

## 1. Quick start

Prerequisites: **JDK 21** and **Maven 3.9+** (Docker is optional, only for RabbitMQ/Elasticsearch).

```bash
git clone <your-repo-url> && cd <your-repo>
mvn clean test -Dgroups=smoke      # first run downloads Playwright browsers (~150 MB)
mvn allure:serve                   # opens the report
```

Make it your own project (renames package, groupId and artifactId; use Git Bash or WSL on Windows):

```bash
./scripts/new-project.sh com.acme.qa acme-qa-tests
```

Optional but recommended, once: add the Maven wrapper so nobody needs Maven installed: `mvn wrapper:wrapper`.

## 2. Everyday commands

| Goal | Command |
|---|---|
| Fast smoke suite | `mvn clean test -Dgroups=smoke` |
| Everything | `mvn clean test` |
| Only one layer | `-Dgroups=ui` / `-Dgroups=api` / `-Dgroups=db` |
| Tag expression | `-Dgroups="smoke & api"` (quote it in PowerShell) |
| One test class | `-Dtest=LoginUiTest` |
| Other environment | `-Denv=staging` (loads `config/staging.properties` over `qa.properties`) |
| See the browser | `-Dheadless=false` |
| Other browser | `-Dbrowser=firefox` or `webkit` |
| Retry failures once | `-Drerun.count=1` (retried tests show as flaky in Allure) |
| RabbitMQ + Elasticsearch tests | `docker compose up -d` then `-Drabbit.enabled=true -Delastic.enabled=true` |
| Run in Docker | `docker build -t qa-tests . && docker run --rm qa-tests` |

Tests that need a real external service skip themselves when it is not configured (for example `UserApiTest` needs `api.key`), so a fresh clone is always green.

## 3. Project layout

```
src/main/java/.../
  config/        typed config interfaces (Owner): FrameworkConfig, DatabaseConfig, RabbitConfig, ElasticConfig
  core/browser/  PlaywrightManager (thread-safe sessions) + PlaywrightExtension (screenshot + trace on failure)
  core/api/      ApiSpecificationFactory (RestAssured base spec, Allure, Jackson)
  core/db/       DatabaseManager (HikariCP + JDBI)
  core/messaging RabbitMqClient: declare/bind/publish/get/wait/purge/delete
  core/search/   ElasticClient + ElasticQueryBuilder (fluent) + SearchResult
  core/factory/  ClientFactory, PageFactory: the only places that create clients and pages
  clients/       one class per API: methods return Response, no assertions
  pages/         Page Objects (Playwright), no assertions
  repositories/  DB access interfaces (JDBI SqlObject)
  models/        records for request/response/document models (UserRecord, LogEntry + builder)
  utils/         JsonUtils, Secrets, WaitUtils (Awaitility), TestDataFactory (Instancio + Datafaker)
src/test/java/.../
  base/          BaseUiTest, TestLifecycleLogger
  ui/ api/ db/ integration/   the tests
src/test/resources/
  config/        qa.properties, staging.properties
  allure/        categories.json, environment.properties (copied into the report)
  schemas/       JSON schemas for contract checks
  junit-platform.properties   parallel execution
  logback-test.xml            console + logs/test-run.log
```

Rule of thumb: **tests assert, clients/pages/repositories act.** See [docs/WRITING-TESTS.md](docs/WRITING-TESTS.md) for copy-paste examples of each layer.

## 4. Configuration and secrets

- Priority: `-Dkey=value` > environment variables > `config/<env>.properties` > `config/qa.properties`.
- Secrets never go into the repo. For `api.key`, `db.password`, `rabbit.password`, `elastic.password` use `-D...` or an env var named like the key in upper case with underscores (`API_KEY`, `DB_PASSWORD`, ...). See `.env.example`. In CI use GitHub Secrets.
- To use PostgreSQL instead of H2, override `db.*` in `staging.properties`.

## 5. Reports and debugging

- **Allure:** `mvn allure:serve`. Steps from page objects/clients, request/response logs, environment widget, failure categories.
- **Playwright trace (UI failures):** saved to `target/traces/*.zip` and attached to the Allure report. Open it at https://trace.playwright.dev for a step-by-step replay with DOM snapshots and network calls.
- **Logs:** `logs/test-run.log` (rolling). Every test writes START/PASSED/FAILED lines; the `[thread]` column separates parallel tests.

## 6. CI (GitHub Actions)

`.github/workflows/ci-pipeline.yml`: pull requests run the **smoke** suite, pushes to `main`, the nightly schedule and manual runs run everything (or the tag expression you enter). Failed tests are retried once; Allure report, traces and logs are uploaded as artifacts. Add the repository secret `API_KEY` if you want the real-service API tests to run. Dependabot keeps Maven and Action versions fresh.

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| `TypeTag :: UNKNOWN` or other compiler crash | Use JDK 21 (Project SDK + Maven runner JRE in IntelliJ) |
| `sun.misc.Unsafe` warning on JDK 24+ | Harmless; the `jdk23plus` profile already silences it |
| Browser not found / first run slow | Playwright downloads browsers once; or run the install command from the CI file |
| API tests are skipped | Set `api.key` (reqres.in needs one) |
| No Allure report | Run tests first, then `mvn allure:serve` |

More conventions: [CONTRIBUTING.md](CONTRIBUTING.md).
