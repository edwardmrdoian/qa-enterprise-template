package com.company.qa.core.api;

import com.company.qa.config.FrameworkConfig;
import com.company.qa.utils.JsonUtils;
import com.company.qa.utils.Secrets;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class ApiSpecificationFactory {

    private ApiSpecificationFactory() {}

    public static RequestSpecification baseSpec() {
        return baseSpec(FrameworkConfig.get().apiBaseUrl());
    }

    /** Use this overload to point a client at a mock server (WireMock) or another service. */
    public static RequestSpecification baseSpec(String baseUri) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(RestAssuredConfig.config().objectMapperConfig(
                        new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> JsonUtils.MAPPER)))
                .addFilter(new AllureRestAssured())
                .log(LogDetail.URI)
                .log(LogDetail.METHOD);

        String apiKey = apiKey();
        if (!apiKey.isBlank()) {
            builder.addHeader("x-api-key", apiKey);
        }
        return builder.build();
    }

    public static RequestSpecification authorizedSpec(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(baseSpec())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    public static String apiKey() {
        return Secrets.get("api.key", FrameworkConfig.get().apiKey());
    }
}
