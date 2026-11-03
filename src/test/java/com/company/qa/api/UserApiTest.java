package com.company.qa.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.qa.clients.UserApiClient;
import com.company.qa.core.api.ApiSpecificationFactory;
import com.company.qa.models.UserRecord;
import com.company.qa.utils.TestDataFactory;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Runs against the real service (reqres.in). Skipped automatically when no API key is configured.
 */
@Feature("Users API (real service)")
@Tag("api")
@Tag("regression")
@Tag("example")
@Tag("external")
class UserApiTest {

    @BeforeEach
    void requireCredentials() {
        if (ApiSpecificationFactory.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "External API examples require API_KEY or -Dapi.key. Use the default framework suite for offline tests.");
        }
    }

    private final UserApiClient client = new UserApiClient();

    @Test
    @DisplayName("POST /users creates a user")
    void createUser() {
        UserRecord user = TestDataFactory.createRandomUser();

        Response response = client.createUser(user);

        assertThat(response.statusCode()).isEqualTo(201);
        UserRecord created = response.as(UserRecord.class);
        assertThat(created.name()).isEqualTo(user.name());
        assertThat(created.job()).isEqualTo(user.job());
        assertThat(created.id()).isNotBlank();
    }

    @Test
    @DisplayName("GET /users/2 returns a user")
    void getUser() {
        Response response = client.getUser("2");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.id")).isEqualTo("2");
    }

    @Test
    @DisplayName("GET /users/23 returns 404")
    void getMissingUser() {
        assertThat(client.getUser("23").statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("DELETE /users/2 returns 204")
    void deleteUser() {
        assertThat(client.deleteUser("2").statusCode()).isEqualTo(204);
    }
}
