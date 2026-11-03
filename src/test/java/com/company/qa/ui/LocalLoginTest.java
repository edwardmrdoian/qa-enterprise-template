package com.company.qa.ui;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.company.qa.base.BaseUiTest;
import com.company.qa.pages.LoginPage;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.microsoft.playwright.Page;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("framework")
@Tag("smoke")
@Tag("ui")
class LocalLoginTest extends BaseUiTest {
    @RegisterExtension
    final WireMockExtension server =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .failOnUnmatchedRequests(true)
                    .build();

    @BeforeEach
    void configureFixture(Page page) throws IOException {
        try (var input = getClass().getResourceAsStream("/fixtures/login.html")) {
            if (input == null) throw new IllegalStateException("Missing local login fixture");
            server.stubFor(
                    get(urlEqualTo("/login"))
                            .willReturn(
                                    ok().withHeader("Content-Type", "text/html")
                                            .withBody(
                                                    new String(
                                                            input.readAllBytes(),
                                                            StandardCharsets.UTF_8))));
        }
        server.stubFor(get(urlEqualTo("/favicon.ico")).willReturn(aResponse().withStatus(204)));
        page.navigate(server.url("/login"));
    }

    @Test
    void validLoginUsesTheRealBrowserAgainstLocalHttp(Page page) {
        server.stubFor(
                post(urlEqualTo("/login"))
                        .withRequestBody(containing("username=tomsmith"))
                        .willReturn(aResponse().withStatus(303).withHeader("Location", "/secure")));
        server.stubFor(
                get(urlEqualTo("/secure"))
                        .willReturn(
                                ok().withHeader("Content-Type", "text/html")
                                        .withBody(
                                                "<h1>Secure area</h1><div id='flash'>You logged in!</div>")));

        new LoginPage(page).loginAs("tomsmith", "test-only-password");

        assertThat(page.locator("#flash")).hasText("You logged in!");
        assertThat(page).hasURL(server.url("/secure"));
        server.verify(postRequestedFor(urlEqualTo("/login")));
    }

    @Test
    void invalidLoginShowsTheLocalError(Page page) {
        server.stubFor(
                post(urlEqualTo("/login"))
                        .willReturn(
                                ok().withHeader("Content-Type", "text/html")
                                        .withBody("<div id='flash'>Invalid credentials</div>")));
        new LoginPage(page).loginAs("wrong", "test-only-password");
        assertThat(page.locator("#flash")).hasText("Invalid credentials");
    }
}
