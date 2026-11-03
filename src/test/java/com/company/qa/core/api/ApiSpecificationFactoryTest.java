package com.company.qa.core.api;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("framework")
class ApiSpecificationFactoryTest {
    @RegisterExtension
    final WireMockExtension server =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @Test
    void customEndpointDoesNotReceiveGlobalCredentials() {
        server.stubFor(get(urlEqualTo("/check")).willReturn(ok()));
        given().spec(ApiSpecificationFactory.baseSpec(server.baseUrl()))
                .get("/check")
                .then()
                .statusCode(200);
        server.verify(getRequestedFor(urlEqualTo("/check")).withoutHeader("x-api-key"));
    }

    @Test
    void credentialsMustBeExplicitForCustomEndpoints() throws java.io.IOException {
        server.stubFor(get(urlPathEqualTo("/check")).willReturn(ok()));
        given().spec(ApiSpecificationFactory.baseSpec(server.baseUrl(), "test-only-marker"))
                .queryParam("token", "test-only-query")
                .get("/check")
                .then()
                .statusCode(200);
        server.verify(
                getRequestedFor(urlPathEqualTo("/check"))
                        .withHeader("x-api-key", equalTo("test-only-marker")));
        java.util.List<io.qameta.allure.model.Attachment> attachments = new java.util.ArrayList<>();
        io.qameta.allure.Allure.getLifecycle()
                .updateTestCase(result -> attachments.addAll(result.getAttachments()));
        assertThat(attachments).isNotEmpty();
        for (var attachment : attachments) {
            String text =
                    java.nio.file.Files.readString(
                            java.nio.file.Path.of(
                                    System.getProperty("allure.results.directory"),
                                    attachment.getSource()));
            assertThat(text)
                    .doesNotContain("test-only-marker", "test-only-query", "x-api-key", "?token=");
        }
    }

    @Test
    void timeoutsAreAppliedAndInvalidAuthenticationIsRejected() {
        var params =
                io.restassured.specification.SpecificationQuerier.query(
                                ApiSpecificationFactory.baseSpec(server.baseUrl()))
                        .getConfig()
                        .getHttpClientConfig()
                        .params();
        assertThat(params.get("http.connection.timeout")).isEqualTo(10000);
        assertThat(params.get("http.socket.timeout")).isEqualTo(30000);
        assertThatThrownBy(() -> ApiSpecificationFactory.authorizedSpec(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> ApiSpecificationFactory.baseSpec("https://user:password@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
