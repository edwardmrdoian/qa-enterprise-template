package com.company.qa.core.search;

import static io.restassured.RestAssured.given;

import com.company.qa.config.ElasticConfig;
import com.company.qa.core.api.ApiSpecificationFactory;
import com.company.qa.utils.JsonUtils;
import com.company.qa.utils.WaitUtils;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch helper over the REST API (no extra dependency, goes through RestAssured + Allure).
 */
public class ElasticClient {
    private final String baseUrl;

    public ElasticClient() {
        this.baseUrl = null;
    }

    public ElasticClient(String baseUrl) {
        this.baseUrl = java.util.Objects.requireNonNull(baseUrl, "baseUrl");
    }

    private RequestSpecification spec() {
        ElasticConfig cfg = ElasticConfig.get();
        RequestSpecBuilder builder =
                new RequestSpecBuilder()
                        .addRequestSpecification(
                                ApiSpecificationFactory.baseSpec(
                                        baseUrl == null ? cfg.url() : baseUrl));
        if (baseUrl == null && !cfg.username().isBlank()) {
            builder.setAuth(RestAssured.preemptive().basic(cfg.username(), cfg.password()));
        }
        return builder.build();
    }

    // ---------- index management ----------

    public Response createIndex(String index, Map<String, Object> mappingsAndSettings) {
        return ok(
                given().spec(spec()).body(JsonUtils.toJson(mappingsAndSettings)).put("/" + index));
    }

    @Step("Delete index {index}")
    public void deleteIndex(String index) {
        Response r = given().spec(spec()).delete("/" + index);
        if (r.statusCode() != 404) ok(r);
    }

    @Step("Check index exists {index}")
    public boolean indexExists(String index) {
        Response response = given().spec(spec()).head("/" + index);
        if (response.statusCode() == 404) return false;
        if (response.statusCode() == 200) return true;
        throw new ElasticException(
                "Expected 200 or 404 checking index; received " + response.statusCode());
    }

    @Step("Refresh index {index}")
    public void refresh(String index) {
        ok(given().spec(spec()).post("/" + index + "/_refresh"));
    }

    // ---------- documents ----------

    /** Indexes a document with an auto-generated id and returns that id. */
    public String index(String index, Object document) {
        Response r =
                ok(
                        given().spec(spec())
                                .body(JsonUtils.toJson(document))
                                .post("/" + index + "/_doc"));
        return r.jsonPath().getString("_id");
    }

    public void index(String index, String id, Object document) {
        ok(given().spec(spec()).body(JsonUtils.toJson(document)).put("/" + index + "/_doc/" + id));
    }

    @Step("Get document {id} from {index}")
    public Response getDocument(String index, String id) {
        return given().spec(spec()).get("/" + index + "/_doc/" + id);
    }

    @Step("Delete document {id} from {index}")
    public void deleteDocument(String index, String id) {
        ok(given().spec(spec()).delete("/" + index + "/_doc/" + id));
    }

    // ---------- search ----------

    public SearchResult search(String index, ElasticQueryBuilder query) {
        Response r = ok(given().spec(spec()).body(query.toJson()).post("/" + index + "/_search"));
        long total = r.jsonPath().getLong("hits.total.value");
        List<String> ids = r.jsonPath().getList("hits.hits._id");
        List<Map<String, Object>> sources = r.jsonPath().getList("hits.hits._source");
        return new SearchResult(total, ids, sources);
    }

    public long count(String index, ElasticQueryBuilder query) {
        Response r =
                ok(
                        given().spec(spec())
                                .body(JsonUtils.toJson(query.buildQuery()))
                                .post("/" + index + "/_count"));
        return r.jsonPath().getLong("count");
    }

    public long deleteByQuery(String index, ElasticQueryBuilder query) {
        Response r =
                ok(
                        given().spec(spec())
                                .body(JsonUtils.toJson(query.buildQuery()))
                                .post("/" + index + "/_delete_by_query?refresh=true"));
        return r.jsonPath().getLong("deleted");
    }

    /**
     * Polls until at least minHits documents match (Elasticsearch is near-real-time, ~1s refresh).
     */
    public SearchResult waitForHits(
            String index, ElasticQueryBuilder query, int minHits, Duration timeout) {
        if (minHits <= 0) throw new IllegalArgumentException("minHits must be positive");
        java.util.concurrent.atomic.AtomicReference<SearchResult> found =
                new java.util.concurrent.atomic.AtomicReference<>();
        WaitUtils.poll(timeout)
                .alias("hits in " + index)
                .until(
                        () -> {
                            SearchResult result = search(index, query);
                            if (result.total() < minHits) return false;
                            found.set(result);
                            return true;
                        });
        return found.get();
    }

    // ---------- internals ----------

    private Response ok(Response response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ElasticException("Elasticsearch returned HTTP " + response.statusCode());
        }
        return response;
    }
}
