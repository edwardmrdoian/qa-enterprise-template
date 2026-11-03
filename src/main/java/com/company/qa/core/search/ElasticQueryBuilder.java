package com.company.qa.core.search;

import com.company.qa.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for Elasticsearch search bodies (bool query + paging + sorting). Usage:
 * ElasticQueryBuilder.create().filterTerm("service.keyword", "payments").mustMatch("message",
 * "timeout").size(20)
 */
public final class ElasticQueryBuilder {

    private final List<Object> must = new ArrayList<>();
    private final List<Object> filter = new ArrayList<>();
    private final List<Object> mustNot = new ArrayList<>();
    private final List<Object> should = new ArrayList<>();
    private final List<Object> sort = new ArrayList<>();
    private Integer size;
    private Integer from;

    private ElasticQueryBuilder() {}

    public static ElasticQueryBuilder create() {
        return new ElasticQueryBuilder();
    }

    // ----- scoring clauses -----

    public ElasticQueryBuilder mustMatch(String field, Object value) {
        must.add(Map.of("match", Map.of(field, value)));
        return this;
    }

    public ElasticQueryBuilder mustMatchPhrase(String field, String phrase) {
        must.add(Map.of("match_phrase", Map.of(field, phrase)));
        return this;
    }

    public ElasticQueryBuilder mustQueryString(String queryString) {
        must.add(Map.of("query_string", Map.of("query", queryString)));
        return this;
    }

    public ElasticQueryBuilder shouldMatch(String field, Object value) {
        should.add(Map.of("match", Map.of(field, value)));
        return this;
    }

    // ----- non-scoring filters (exact values: use the .keyword field for text) -----

    public ElasticQueryBuilder filterTerm(String field, Object value) {
        filter.add(Map.of("term", Map.of(field, value)));
        return this;
    }

    public ElasticQueryBuilder filterTerms(String field, Collection<?> values) {
        filter.add(Map.of("terms", Map.of(field, values)));
        return this;
    }

    /** Either bound may be null. */
    public ElasticQueryBuilder filterRange(String field, Object gte, Object lte) {
        Map<String, Object> bounds = new LinkedHashMap<>();
        if (gte != null) bounds.put("gte", gte);
        if (lte != null) bounds.put("lte", lte);
        filter.add(Map.of("range", Map.of(field, bounds)));
        return this;
    }

    public ElasticQueryBuilder filterExists(String field) {
        filter.add(Map.of("exists", Map.of("field", field)));
        return this;
    }

    public ElasticQueryBuilder mustNotTerm(String field, Object value) {
        mustNot.add(Map.of("term", Map.of(field, value)));
        return this;
    }

    // ----- paging / sorting -----

    public ElasticQueryBuilder size(int size) {
        if (size < 0) throw new IllegalArgumentException("size must not be negative");
        this.size = size;
        return this;
    }

    public ElasticQueryBuilder from(int from) {
        if (from < 0) throw new IllegalArgumentException("from must not be negative");
        this.from = from;
        return this;
    }

    public ElasticQueryBuilder sortBy(String field, boolean ascending) {
        sort.add(Map.of(field, Map.of("order", ascending ? "asc" : "desc")));
        return this;
    }

    // ----- output -----

    /** Only the "query" part. Used by _count and _delete_by_query. */
    public Map<String, Object> buildQuery() {
        Map<String, Object> bool = new LinkedHashMap<>();
        if (!must.isEmpty()) bool.put("must", must);
        if (!filter.isEmpty()) bool.put("filter", filter);
        if (!mustNot.isEmpty()) bool.put("must_not", mustNot);
        if (!should.isEmpty()) {
            bool.put("should", should);
            bool.put("minimum_should_match", 1);
        }
        Map<String, Object> query =
                bool.isEmpty() ? Map.of("match_all", Map.of()) : Map.of("bool", bool);
        return Map.of("query", query);
    }

    /** Full search body including size, from and sort. */
    public Map<String, Object> build() {
        Map<String, Object> body = new LinkedHashMap<>(buildQuery());
        if (size != null) body.put("size", size);
        if (from != null) body.put("from", from);
        if (!sort.isEmpty()) body.put("sort", sort);
        return body;
    }

    public String toJson() {
        return JsonUtils.toJson(build());
    }
}
