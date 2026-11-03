# QA Enterprise Template

A Java **25** starter for maintainable UI, API, database, RabbitMQ and Elasticsearch tests.
The default suite checks the framework using **real local infrastructure or isolated mocks**:
Chromium against a local HTTP fixture, REST Assured against WireMock, and JDBI against H2.
Public demo services and Docker-backed examples are explicitly opt-in.

**Stack:** Maven Wrapper 3.9.9, JUnit 5, Playwright, REST Assured, WireMock, AssertJ,
Awaitility, Jackson, JDBI/HikariCP, Owner, Datafaker/Instancio, Allure and Spotless.

## 1. Create your project

Install **JDK 25** and set `JAVA_HOME`; no global Maven installation is needed.
Use GitHub's **Use this template** button when enabled, or clone into a new repository.
Keep the MIT license notice when redistributing template code.

From the project root, initialize the Java package, Maven coordinates and display name:

```powershell
# Windows (also works in PowerShell on Linux/macOS)
.\scripts\new-project.ps1 com.acme.qa acme-qa-tests "Acme QA Tests"
```

```bash
# Linux/macOS or Git Bash
bash scripts/new-project.sh com.acme.qa acme-qa-tests "Acme QA Tests"
```

Both launch the same Java initializer. Without a scripting shell, run
`java scripts/NewProject.java com.acme.qa acme-qa-tests "Acme QA Tests"` from the root.
It validates inputs and source destinations before writing, updates Java/service-provider
references, and refuses repeated initialization. It does not rewrite Git remotes or history.

Install the matching browser and verify the starter:

```powershell
.\mvnw.cmd -B exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
.\mvnw.cmd clean verify
```

```bash
bash ./mvnw -B exec:java -Dexec.mainClass=com.microsoft.playwright.CLI '-Dexec.args=install --with-deps chromium'
bash ./mvnw clean verify
```

The first run downloads Maven, dependencies and browser binaries. After installation, the
default tests do not need public websites, credentials, Docker or manually started services.
Use `mvnw.cmd` on Windows and `./mvnw` (or `bash ./mvnw`) elsewhere for the commands below.

## 2. Choose tests intentionally

| Goal | Command |
|---|---|
| Default: framework checks plus your own `project` tests | `./mvnw clean verify` |
| Framework checks without browsers | `./mvnw test "-Dtest.groups=framework & !ui"` |
| Local API/DB examples | `./mvnw test -Pexamples` |
| Public demo-site examples; needs network and `API_KEY` | `./mvnw test -Pexternal` |
| RabbitMQ and Elasticsearch examples; needs Docker services | `./mvnw test -Pintegration` |
| Your application tests only | `./mvnw test -Dtest.groups=project` |
| One class, overriding the suite if necessary | `./mvnw test -Dtest=LoginUiTest -Dtest.groups=external` |
| Headed UI / another browser | `./mvnw test -Dheadless=false -Dbrowser=firefox` |
| Format / check formatting | `./mvnw spotless:apply` / `./mvnw spotless:check` |
| Open Allure report | `./mvnw allure:serve` |
| Reproduce generated data | `./mvnw test -Dtest.seed=42` |

Default selection is `framework | project`. Tag application tests `project` and a layer
(`ui`, `api`, `db` or `integration`); add `smoke` / `regression` for finer selection.
Examples are tagged `example`; those needing public sites also have `external`.
Profiles replace the default selection rather than adding every suite.
`-Dtest.groups` accepts JUnit tag expressions. Selecting no tests fails the build.

**Examples are teaching material, not proof your application works.** The API mock examples
are also framework checks because they exercise serialization, schemas and real HTTP requests.
External tests fail with a clear error if credentials are missing; they do not silently skip.
Never disable a failing test merely to make CI green. Fix the issue or explicitly quarantine
it with an owner and reason outside required checks.

Retries default to **zero**. `-Drerun.count=1` is available for investigation, not used by CI.
Install Firefox/WebKit explicitly before selecting them.

## 3. Write application tests

Tests assert behavior; page objects, API clients and repositories perform actions.
Prefer explicit dependencies: `Page` / `BrowserContext` parameters are available in tests
extending `BaseUiTest`, and page objects can receive a `Page` in their constructor.
API clients accept a custom base URL without inheriting the global API key.

```java
@Tag("project")
@Tag("ui")
class CheckoutTest extends BaseUiTest {
    @Test
    void customerCanOpenCheckout(Page page) {
        page.navigate("/checkout");
        PlaywrightAssertions.assertThat(page.getByRole(AriaRole.HEADING))
                .hasText("Checkout");
    }
}
```

Use Playwright's retrying assertions for UI state instead of reading a string and checking it once.
Prefer role/label/test-id locators, reusable component objects and small domain-specific helpers.
Do not add another base-class layer or a generic helper until it removes real repetition.

See [the cookbook](docs/WRITING-TESTS.md) for complete imports, API, database,
messaging, search and parameterized-test examples.

## 4. Configuration and secrets

Priority: **Java system properties > normalized environment variables > selected environment
file > QA defaults**. `BASE_URL` maps to `base.url`, `DB_MAX_POOL_SIZE` to `db.max.pool.size`,
and `API_KEY` to `api.key`. `ENV` or `-Denv` selects `config/<env>.properties`.
Missing environments, invalid URLs/browsers/timeouts and invalid pool settings fail early.
Configuration lives under `src/test/resources/config`.

Create your environment file, then supply application endpoints:

```powershell
$env:BASE_URL = "https://qa.example.com"
$env:API_BASE_URL = "https://api.qa.example.com"
$env:API_KEY = "<obtain-from-your-secret-manager>"
.\mvnw.cmd test -Dtest.groups=project
```

`.env.example` lists supported variables. A `.env` file is **not loaded automatically**:
export values through your shell, IDE or secret manager. Do not put credentials in command
history, tracked files or reports. An explicitly empty secret overrides a lower-priority value.
HTTP connection/read timeouts default to 10/30 seconds. TLS verification stays enabled.

The default local RabbitMQ `guest/guest` credentials are development-only.
**A previously tracked API credential must be revoked/rotated by its owner. Removing it
from current files does not remove it from Git history or make it safe to reuse.**

## 5. Optional local integration services

Requires Docker with Compose v2 (`up --wait` support):

```bash
docker compose up -d --wait --wait-timeout 180
./mvnw clean test -Pintegration
docker compose down --volumes
```

Services bind to loopback only and have readiness checks. Use disposable test infrastructure:
never run destructive examples against shared or production databases, queues or indices.
Examples use unique resources and cleanup even after assertion failures.

Containerized framework checks use JDK 25 and an image matching the Playwright dependency:

```bash
docker build -t qa-tests .
docker run --rm qa-tests
```

Pass Maven arguments after the image name to change the suite. Mount `/tests/target`
if reports must survive container removal. Changing `playwright.version` requires updating
the Docker image tag too. Docker and public-service suites are not prerequisites for normal CI.

## 6. CI and reports

- **QA CI Pipeline:** PRs, pushes to `master`/`main`, and manual runs. Windows and Ubuntu
  run `clean verify`: compilation, default deterministic tests, and formatting checks.
  Initialization is checked in a temporary copy, including compilation and non-UI tests
  after renaming. Required runs never receive external-service credentials or use retries.
- **Optional QA suites:** manual-only local examples, Docker integrations, or public-site
  examples. `API_KEY` is provided only to the external suite. Selected suites must execute
  tests without skips; missing services or credentials cause failures.
- Artifacts retain Surefire results, Allure data/report, failure traces and logs for seven days.

After adopting the template, enable branch protection with both `verify` matrix checks and
require PR review. Adjust push branch names if your default is not `master` or `main`.
Repository settings are not inherited by a clone/template.

Allure records steps and HTTP **metadata only** by default: headers, query values and request/
response bodies are deliberately omitted. Attach additional payloads only after redaction.
UI screenshots and Playwright traces may still contain credentials or personal data; restrict
artifact access and use dedicated test accounts. Open traces locally rather than uploading
sensitive artifacts to public services:

```bash
./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI '-Dexec.args=show-trace target/traces/<file>.zip'
```

Trace names are unique per invocation, including parameterized tests/retries. Test-body failure
evidence is captured before teardown; setup/teardown failures are captured when a session is available.
Attachment failures are logged without replacing the original test failure.
Framework checks intentionally exercise failure paths in isolated test-engine fixtures; those checks
pass only when screenshots, distinct traces and session cleanup are correct.
Logs are in `logs/test-run.log`; generated-data seeds are recorded for reproduction.

## 7. Troubleshooting

| Symptom | Action |
|---|---|
| Java version error | Set `JAVA_HOME` and IDE Maven runner to JDK 25 |
| Browser executable missing | Run the explicit Playwright install command above |
| No tests matched | Check `test.groups`, profile and `-Dtest`; an empty selection is an error |
| External API key missing | Supply `API_KEY`, or use the default local/mock suite |
| Missing environment file | Add `config/<env>.properties`; unknown names do not fall back silently |
| Integration connection failure | Run Compose with `--wait`; inspect `docker compose logs` |
| Formatting check failure | Run `./mvnw spotless:apply` |
| Native-access / Unsafe warnings on Java 25 | Some pinned dependencies emit compatibility warnings; these are not test failures |

See [CONTRIBUTING.md](CONTRIBUTING.md) for development conventions. Licensed under [MIT](LICENSE).
