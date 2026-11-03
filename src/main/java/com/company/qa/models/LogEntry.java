package com.company.qa.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** A typical ELK log document. Build with LogEntry.builder() and sensible defaults. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LogEntry(
        @JsonProperty("@timestamp") Instant timestamp,
        String level,
        String service,
        String message,
        String traceId,
        Map<String, Object> fields) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .timestamp(timestamp)
                .level(level)
                .service(service)
                .message(message)
                .traceId(traceId)
                .fields(fields);
    }

    public static final class Builder {
        private Instant timestamp = Instant.now();
        private String level = "INFO";
        private String service = "qa-service";
        private String message = "test message";
        private String traceId = UUID.randomUUID().toString();
        private final Map<String, Object> fields = new LinkedHashMap<>();

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder field(String key, Object value) {
            this.fields.put(key, value);
            return this;
        }

        public Builder fields(Map<String, Object> fields) {
            if (fields != null) this.fields.putAll(fields);
            return this;
        }

        public LogEntry build() {
            return new LogEntry(timestamp, level, service, message, traceId, Map.copyOf(fields));
        }
    }
}
