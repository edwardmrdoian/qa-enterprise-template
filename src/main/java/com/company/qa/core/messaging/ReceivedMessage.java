package com.company.qa.core.messaging;

import com.company.qa.utils.JsonUtils;
import java.util.Map;

public record ReceivedMessage(
        String body, Map<String, Object> headers, String exchange, String routingKey) {

    public <T> T as(Class<T> type) {
        return JsonUtils.fromJson(body, type);
    }
}
