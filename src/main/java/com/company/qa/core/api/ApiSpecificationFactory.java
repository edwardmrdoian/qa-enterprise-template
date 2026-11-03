package com.company.qa.core.api;

import com.company.qa.config.FrameworkConfig;
import com.company.qa.utils.JsonUtils;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class ApiSpecificationFactory {

    private ApiSpecificationFactory() {}

    public static RequestSpecification baseSpec() {
        return baseSpec(FrameworkConfig.get().apiBaseUrl(), apiKey());
    }

    /** Use this overload to point a client at a mock server (WireMock) or another service. */
    public static RequestSpecification baseSpec(String baseUri) {
        return baseSpec(baseUri, "");
    }

    public static RequestSpecification baseSpec(String baseUri, String apiKey) {
        java.net.URI uri = java.net.URI.create(baseUri);
        if (uri.getHost() == null
                || uri.getUserInfo() != null
                || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException(
                    "API base URI must be absolute HTTP(S) without embedded credentials");
        }
        java.util.Objects.requireNonNull(apiKey, "apiKey");
        FrameworkConfig cfg = FrameworkConfig.get();
        RequestSpecBuilder builder =
                new RequestSpecBuilder()
                        .setBaseUri(baseUri)
                        .setContentType(ContentType.JSON)
                        .setAccept(ContentType.JSON)
                        .setConfig(
                                RestAssuredConfig.config()
                                        .httpClient(
                                                HttpClientConfig.httpClientConfig()
                                                        .setParam(
                                                                "http.connection.timeout",
                                                                cfg.apiConnectTimeoutMs())
                                                        .setParam(
                                                                "http.socket.timeout",
                                                                cfg.apiReadTimeoutMs()))
                                        .objectMapperConfig(
                                                new ObjectMapperConfig()
                                                        .jackson2ObjectMapperFactory(
                                                                (cls, charset) ->
                                                                        JsonUtils.MAPPER)))
                        .addFilter(new SafeApiReporting());

        if (!apiKey.isBlank()) {
            builder.addHeader("x-api-key", apiKey);
        }
        return builder.build();
    }

    public static RequestSpecification authorizedSpec(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Bearer token must not be blank");
        }
        return new RequestSpecBuilder()
                .addRequestSpecification(baseSpec())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    public static String apiKey() {
        return FrameworkConfig.get().apiKey();
    }
}
