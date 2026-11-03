package com.company.qa.core.search;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("framework")
class ElasticClientMockedTest {
    @RegisterExtension
    final WireMockExtension server =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @Test
    void indexExistsDistinguishesMissingIndexFromServiceErrors() {
        server.stubFor(head(urlEqualTo("/missing")).willReturn(aResponse().withStatus(404)));
        server.stubFor(head(urlEqualTo("/forbidden")).willReturn(aResponse().withStatus(401)));
        server.stubFor(head(urlEqualTo("/present")).willReturn(aResponse().withStatus(200)));
        ElasticClient client = new ElasticClient(server.baseUrl());
        assertThat(client.indexExists("missing")).isFalse();
        assertThat(client.indexExists("present")).isTrue();
        assertThatThrownBy(() -> client.indexExists("forbidden"))
                .isInstanceOf(ElasticException.class)
                .hasMessageContaining("401");
    }

    @Test
    void waitReturnsTheObservedResultWithoutASecondSearch() {
        AtomicInteger searches = new AtomicInteger();
        ElasticClient client =
                new ElasticClient() {
                    @Override
                    public SearchResult search(String index, ElasticQueryBuilder query) {
                        searches.incrementAndGet();
                        return new SearchResult(
                                1,
                                java.util.List.of("1"),
                                java.util.List.of(java.util.Map.of("message", "ok")));
                    }
                };
        assertThat(
                        client.waitForHits(
                                        "test",
                                        ElasticQueryBuilder.create(),
                                        1,
                                        Duration.ofSeconds(2))
                                .total())
                .isEqualTo(1);
        assertThat(searches).hasValue(1);
    }

    @Test
    void invalidQueriesFailImmediatelyRatherThanTimingOut() {
        ElasticClient client =
                new ElasticClient() {
                    @Override
                    public SearchResult search(String index, ElasticQueryBuilder query) {
                        throw new ElasticException("Invalid query");
                    }
                };
        assertThatThrownBy(
                        () ->
                                client.waitForHits(
                                        "test",
                                        ElasticQueryBuilder.create(),
                                        1,
                                        Duration.ofSeconds(2)))
                .isInstanceOf(ElasticException.class)
                .hasMessage("Invalid query");
    }

    @Test
    void queryBuilderIncludesFiltersAndRejectsNegativePaging() {
        String json =
                ElasticQueryBuilder.create()
                        .filterTerm("service.keyword", "qa")
                        .filterRange("@timestamp", "now-1h", null)
                        .size(10)
                        .from(0)
                        .toJson();
        assertThat(json).contains("\"term\"", "\"gte\":\"now-1h\"", "\"size\":10");
        assertThatThrownBy(() -> ElasticQueryBuilder.create().size(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ElasticQueryBuilder.create().from(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
