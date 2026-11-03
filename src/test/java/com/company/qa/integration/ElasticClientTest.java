package com.company.qa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.qa.core.factory.ClientFactory;
import com.company.qa.core.search.ElasticClient;
import com.company.qa.core.search.ElasticQueryBuilder;
import com.company.qa.core.search.SearchResult;
import com.company.qa.models.LogEntry;
import io.qameta.allure.Feature;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Feature("Elasticsearch")
@Tag("integration")
@Tag("example")
@Tag("regression")
class ElasticClientTest {

    private final ElasticClient elastic = ClientFactory.elastic();

    @Test
    @DisplayName("Index a log entry and find it by trace id")
    void indexAndSearch() {
        String index = "qa-logs-" + UUID.randomUUID().toString().substring(0, 8);
        LogEntry entry =
                LogEntry.builder()
                        .service("payments")
                        .level("ERROR")
                        .message("Gateway timeout while charging card")
                        .field("orderId", 1001)
                        .build();
        try {
            elastic.index(index, entry);

            ElasticQueryBuilder query =
                    ElasticQueryBuilder.create()
                            .filterTerm("traceId.keyword", entry.traceId())
                            .filterTerm("level.keyword", "ERROR")
                            .size(10);

            SearchResult result = elastic.waitForHits(index, query, 1, Duration.ofSeconds(15));

            assertThat(result.total()).isEqualTo(1);
            LogEntry found = result.as(LogEntry.class).get(0);
            assertThat(found.service()).isEqualTo("payments");
            assertThat(found.message()).contains("timeout");
        } finally {
            elastic.deleteIndex(index);
        }
    }
}
