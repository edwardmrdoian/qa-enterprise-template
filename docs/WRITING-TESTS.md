# Writing tests

Use the initialized package in place of `com.company.qa` below. Examples already present
in the repository demonstrate complete tests; they are not all part of default CI.

## UI: injected pages and retrying assertions

```java
import com.company.qa.base.BaseUiTest;
import com.company.qa.pages.LoginPage;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Tag("project")
@Tag("ui")
class LoginTest extends BaseUiTest {
    @Test
    void invalidCredentialsShowAnError(Page page) {
        new LoginPage(page).open().loginAs("invalid", "test-only-password");
        assertThat(page.locator("#flash")).containsText("invalid");
    }
}
```

`BaseUiTest` creates one browser/context/page per test and thread, injects `Page` or
`BrowserContext` into test/setup methods, and collects failure evidence. Construct page
objects after the session starts, not in test field initializers.
Prefer role/label/test-id locators. Use component objects for repeated widgets.
`LocalLoginTest` shows a real browser against an isolated localhost HTTP fixture without
an external website. `LoginUiTest` is an opt-in public-site example.

## Parameterized tests

```java
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("project")
@Tag("api")
class StatusMappingTest {
    @ParameterizedTest(name = "{0} is successful: {1}")
    @CsvSource({"200,true", "201,true", "404,false"})
    void classifiesStatusCodes(int status, boolean expected) {
        assertThat(status >= 200 && status < 300).isEqualTo(expected);
    }
}
```

Prefer boundary/negative cases meaningful to your application over large random matrices.

## API: isolated mocks and explicit authentication

Copy `UserApiMockedTest` for a per-test WireMock extension on a dynamic port.
Create the client in `@BeforeEach`, after WireMock starts:

```java
UserApiClient client = ClientFactory.userApi(wm.baseUrl());
Response response = client.getUser("2");
response.then().statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/user-response.json"));
```

Relevant imports: `com.company.qa.clients.UserApiClient`,
`com.company.qa.core.factory.ClientFactory`, `io.restassured.response.Response`, and static
`io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath`.

Custom base URLs receive **no global API key**. For authenticated endpoints use
`ApiSpecificationFactory.baseSpec(url, key)` or supply a bearer header explicitly.
The default configured service uses `API_KEY`; store real credentials in your secret manager.
HTTP requests have explicit connection/read timeouts and metadata-only reporting.

Mock tests prove client behavior, not the real backend's contract. Add a small, deliberately
selected real-service suite once the actual application's endpoints and data lifecycle are known.

## Reproducible data

```java
UserRecord user = TestDataFactory.user().seed(42)
        .name("QA Engineer").email("qa@example.com").build();
```

Imports: `com.company.qa.models.UserRecord`, `com.company.qa.utils.TestDataFactory`.
Without an explicit seed, a seed is generated and logged. Reproduce one failure with
`-Dtest.seed=<recorded-number>` or `.seed(...)`. `TestDataFactory.create(Model.class, seed)`
uses Instancio for other models. Use unique resource IDs separately; a fixed seed intentionally
generates identical data, so it must not be used as the uniqueness mechanism.
Explicit overrides, including `null`, are preserved for negative cases (for example `.email(null)`).

## Database: owned fixtures

`DatabaseManager` supports try-with-resources and an explicitly supplied `DatabaseConfig`.
`DatabaseManagerTest` creates a unique H2 database, exercises real SQL/record mapping,
and closes the pool after the test. This is safe even if application configuration points elsewhere.

For configured application databases:

```java
try (DatabaseManager database = new DatabaseManager(DatabaseConfig.get())) {
    UserRepository repo = UserRepository.create(database.database());
    try {
        repo.insert(id, name, job, email, "2026-01-01T00:00:00Z");
        WaitUtils.poll().untilAsserted(() -> assertThat(repo.findById(id)).isPresent());
    } finally {
        repo.deleteById(id);
    }
}
```

Imports: `com.company.qa.core.db.DatabaseManager`, `com.company.qa.config.DatabaseConfig`,
`com.company.qa.repositories.UserRepository`, `com.company.qa.utils.WaitUtils`, and static
`org.assertj.core.api.Assertions.assertThat`. The inserted values are test-owned variables.
Add parameterized SQL via `@SqlQuery`/`@SqlUpdate`. Never create/drop schemas in a shared
environment without an explicit test-data agreement. The static default pool remains a
convenience for examples and closes on JVM shutdown; prefer owned pools for reusable fixtures.

## RabbitMQ: ownership, matching and cleanup

```java
try (RabbitMqClient rabbit = ClientFactory.rabbitMq()) {
    rabbit.declareQueue(queue);
    try {
        rabbit.publish(queue, payload);
        ReceivedMessage message = rabbit.waitForMessage(queue, Duration.ofSeconds(10));
        assertThat(message.as(Payload.class)).isEqualTo(payload);
    } finally {
        rabbit.deleteQueue(queue);
    }
}
```

Imports: `com.company.qa.core.messaging.RabbitMqClient`,
`com.company.qa.core.messaging.ReceivedMessage`, `com.company.qa.core.factory.ClientFactory`,
`java.time.Duration`, and static AssertJ `assertThat`. `Payload`, `payload` and unique `queue`
belong to your application/test.

Use one client per test; channels are not thread-safe. The predicate overload holds unmatched
deliveries unacknowledged while scanning and requeues them when it finishes (including on
timeout/matcher failure), so an unmatched head cannot starve a later match. Use dedicated queues:
matching consumes/acknowledges the selected message, and requeueing may change order.
Queue/exchange cleanup belongs in nested `finally` blocks; see `RabbitMqClientTest`.

## Elasticsearch: observable state, not hidden errors

```java
ElasticClient elastic = ClientFactory.elastic();
String index = "qa-" + UUID.randomUUID();
try {
    LogEntry entry = LogEntry.builder().service("payments").level("ERROR").message("timeout").build();
    elastic.index(index, entry);
    ElasticQueryBuilder query = ElasticQueryBuilder.create()
            .filterTerm("traceId.keyword", entry.traceId()).size(10);
    SearchResult result = elastic.waitForHits(index, query, 1, Duration.ofSeconds(15));
    assertThat(result.as(LogEntry.class)).hasSize(1);
} finally {
    elastic.deleteIndex(index);
}
```

Imports: search classes from `com.company.qa.core.search`, `com.company.qa.models.LogEntry`,
`com.company.qa.core.factory.ClientFactory`, `java.util.UUID`, `java.time.Duration`, and AssertJ.
Use `.keyword` for exact filters on dynamically mapped text. `new ElasticClient(url)` supports
an isolated mock without forwarding configured credentials.
Polling tolerates missing hits, not malformed queries, authentication failures or arbitrary
exceptions. Configure trust for your environment rather than disabling TLS validation.

## Suite commands

`./mvnw clean verify` runs framework checks and `project` tests.
`-Pexamples` selects local examples; `-Pexternal` selects public services and needs `API_KEY`.
For Docker integrations, run `docker compose up -d --wait --wait-timeout 180`, then
`./mvnw test -Pintegration`, then `docker compose down --volumes`.
Do not combine profiles accidentally; use an explicit `-Dtest.groups` expression when combining suites.
