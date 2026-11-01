package com.company.qa.core.search;

import com.company.qa.utils.JsonUtils;

import java.util.List;
import java.util.Map;

public record SearchResult(long total, List<String> ids, List<Map<String, Object>> sources) {

    public <T> List<T> as(Class<T> type) {
        return sources.stream().map(s -> JsonUtils.convert(s, type)).toList();
    }

    public boolean isEmpty() {
        return sources.isEmpty();
    }
}
