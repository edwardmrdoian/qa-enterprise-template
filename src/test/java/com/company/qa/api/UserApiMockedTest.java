package com.company.qa.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.company.qa.clients.UserApiClient;
import com.company.qa.models.UserRecord;
import com.company.qa.utils.TestDataFactory;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Self-contained example: no internet, no keys. Shows WireMock stubbing, request verification,
 * JSON-schema contract validation and AssertJ soft assertions.
 */
@Feature("Users API (mocked)")
@Tag("api")
@Tag("smoke")
@Tag("framework")
@Tag("example")
class UserApiMockedTest {

    @RegisterExtension
    final WireMockExtension wm =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private UserApiClient client;

    @BeforeEach
    void createClient() {
        client = com.company.qa.core.factory.ClientFactory.userApi(wm.baseUrl());
    }

    @Test
    @DisplayName("GET /users/2 response matches the JSON schema")
    void getUserMatchesSchema() {
        wm.stubFor(
                get(urlEqualTo("/api/users/2"))
                        .willReturn(
                                okJson(
                                        """
                {"data":{"id":2,"email":"janet.weaver@example.com","first_name":"Janet","last_name":"Weaver"}}
                """)));

        client.getUser("2")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/user-response.json"));
    }

    @Test
    @DisplayName("POST /users sends the model as JSON and parses the created user")
    void createUserSendsCorrectBody() {
        wm.stubFor(
                post(urlEqualTo("/api/users"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                        {"id":"101","name":"Janet","job":"QA Lead","createdAt":"2026-01-01T00:00:00Z"}
                        """)));
        UserRecord user = TestDataFactory.createRandomUser();

        Response response = client.createUser(user);

        wm.verify(
                postRequestedFor(urlEqualTo("/api/users"))
                        .withRequestBody(matchingJsonPath("$.name", equalTo(user.name()))));

        UserRecord created = response.as(UserRecord.class);
        assertSoftly(
                soft -> {
                    soft.assertThat(response.statusCode()).isEqualTo(201);
                    soft.assertThat(created.id()).isEqualTo("101");
                    soft.assertThat(created.job()).isEqualTo("QA Lead");
                });
    }

    @Test
    @DisplayName("GET unknown user returns 404")
    void unknownUserReturns404() {
        wm.stubFor(
                get(urlEqualTo("/api/users/23"))
                        .willReturn(aResponse().withStatus(404).withBody("{}")));

        assertThat(client.getUser("23").statusCode()).isEqualTo(404);
    }
}
