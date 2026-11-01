package com.company.qa.db;

import com.company.qa.core.db.DatabaseManager;
import com.company.qa.models.UserRecord;
import com.company.qa.repositories.UserRepository;
import com.company.qa.utils.TestDataFactory;
import com.company.qa.utils.WaitUtils;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("User persistence")
@Tag("db")
@Tag("regression")
class UserRepositoryTest {

    private static UserRepository repo;

    @BeforeAll
    static void initSchema() {
        DatabaseManager.executeUpdate("""
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
        UserRecord u = TestDataFactory.createRandomUser();
        String id = UUID.randomUUID().toString();

        repo.insert(id, u.name(), u.job(), u.email(), "2026-01-01T00:00:00Z");

        WaitUtils.poll().untilAsserted(() ->
                assertThat(repo.findById(id)).isPresent()
                        .get().extracting(UserRecord::email).isEqualTo(u.email()));

        repo.deleteById(id);
        assertThat(repo.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Count by email")
    void countByEmail() {
        UserRecord u = TestDataFactory.createRandomUser();
        String id = UUID.randomUUID().toString();
        repo.insert(id, u.name(), u.job(), u.email(), "2026-01-01T00:00:00Z");

        assertThat(repo.countByEmail(u.email())).isEqualTo(1);
        repo.deleteById(id);
    }
}
