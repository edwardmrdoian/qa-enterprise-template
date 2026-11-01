# Writing tests: copy-paste cookbook

## UI (Playwright)

1. Page object in `src/main/.../pages`:

```java
public class SecureAreaPage extends BasePage {
    public String heading() { return locator("h2").innerText(); }
    public LoginPage logout() { locator("a.button").click(); return new LoginPage(); }
}
```

2. Test in `src/test/.../ui` (extend `BaseUiTest`, which starts and stops the browser and records traces):

```java
@Feature("Login") @Tag("ui") @Tag("regression")
class LogoutUiTest extends BaseUiTest {
    @Test
    void userCanLogOut() {
        new LoginPage().open().loginAs("tomsmith", "SuperSecretPassword!");
        LoginPage login = new SecureAreaPage().logout();
        assertThat(login.flashMessage()).contains("You logged out");
    }
}
```

Create pages with `PageFactory.create(SecureAreaPage.class)` if you prefer a factory.

## API

Add an endpoint method to a client (or create a new client class), then:

```java
Response r = ClientFactory.userApi().getUser("2");
assertThat(r.statusCode()).isEqualTo(200);
r.then().body(matchesJsonSchemaInClasspath("schemas/user-response.json"));
```

Without a real backend, stub it with WireMock exactly like `UserApiMockedTest` does and construct `new UserApiClient(wm.baseUrl())`.

Test data: `UserRecord user = TestDataFactory.createRandomUser();`

## Database

```java
UserRepository repo = UserRepository.create();
repo.insert(id, name, job, email, "2026-01-01T00:00:00Z");
WaitUtils.poll().untilAsserted(() -> assertThat(repo.findById(id)).isPresent());
```

Add queries as annotated methods on a repository interface (`@SqlQuery`, `@SqlUpdate`). Default DB is in-memory H2; point `db.*` to PostgreSQL for real environments.

## RabbitMQ

```java
try (RabbitMqClient rabbit = ClientFactory.rabbitMq()) {
    rabbit.declareQueue(queue);
    rabbit.publish(queue, payloadObject);                        // objects are sent as JSON
    ReceivedMessage m = rabbit.waitForMessage(queue, Duration.ofSeconds(10));
    assertThat(m.as(Payload.class)).isEqualTo(payloadObject);
    rabbit.deleteQueue(queue);
}
```

Exchanges: `declareExchange`, `bindQueue`, `publish(exchange, routingKey, body, headers)`. Use the predicate overload of `waitForMessage` to pick a specific message.

## Elasticsearch / ELK

```java
ElasticClient elastic = ClientFactory.elastic();
LogEntry entry = LogEntry.builder().service("payments").level("ERROR").message("timeout").build();
elastic.index(index, entry);

ElasticQueryBuilder query = ElasticQueryBuilder.create()
        .filterTerm("traceId.keyword", entry.traceId())
        .filterRange("@timestamp", "now-1h", null)
        .sortBy("@timestamp", false)
        .size(10);

SearchResult result = elastic.waitForHits(index, query, 1, Duration.ofSeconds(15));
assertThat(result.as(LogEntry.class)).hasSize(1);
```

Use `.keyword` fields for exact filters on text fields. Clean up with `elastic.deleteIndex(index)` or `deleteByQuery`.

## Running only integration tests (RabbitMQ / Elasticsearch)

```bash
docker compose up -d
mvn test -Dgroups=integration -Drabbit.enabled=true -Delastic.enabled=true
```
