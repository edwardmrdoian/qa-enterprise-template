package com.company.qa.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.qa.core.db.DatabaseManager;
import com.company.qa.models.UserRecord;
import com.company.qa.repositories.UserRepository;
import com.company.qa.utils.TestDataFactory;
import com.company.qa.utils.WaitUtils;
import io.qameta.allure.Feature;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Feature("User persistence")
@Tag("db")
@Tag("regression")
@Tag("example")
class UserRepositoryTest {

    private static UserRepository repo;

    @BeforeAll
    static void initSchema() {
        DatabaseManager.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(255),
                    job VARCHAR(255),
                    email VARCHAR(255),
                    created_at VARCHAR(64)
                )""");
        repo = UserRepository.create();
    }

    @Test
    @DisplayName("Insert and read back a user")
    void insertAndFind() {
        String id = UUID.randomUUID().toString();
        UserRecord u = TestDataFactory.user().email("qa+" + id + "@example.com").build();

        repo.insert(id, u.name(), u.job(), u.email(), "2026-01-01T00:00:00Z");

        try {
            WaitUtils.poll()
                    .untilAsserted(
                            () ->
                                    assertThat(repo.findById(id))
                                            .isPresent()
                                            .get()
                                            .extracting(UserRecord::email)
                                            .isEqualTo(u.email()));
        } finally {
            repo.deleteById(id);
        }
        assertThat(repo.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Count by email")
    void countByEmail() {
        String id = UUID.randomUUID().toString();
        UserRecord u = TestDataFactory.user().email("qa+" + id + "@example.com").build();
        repo.insert(id, u.name(), u.job(), u.email(), "2026-01-01T00:00:00Z");

        try {
            assertThat(repo.countByEmail(u.email())).isEqualTo(1);
        } finally {
            repo.deleteById(id);
        }
    }
}
