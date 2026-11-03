package com.company.qa.core.db;

import static org.assertj.core.api.Assertions.*;

import com.company.qa.config.DatabaseConfig;
import com.company.qa.repositories.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("framework")
class DatabaseManagerTest {
    @Test
    void ownedLocalPoolSupportsRecordMappingAndExplicitCleanup() {
        DatabaseConfig cfg =
                ConfigFactory.create(
                        DatabaseConfig.class,
                        Map.of(
                                "db.driver",
                                "org.h2.Driver",
                                "db.url",
                                "jdbc:h2:mem:" + UUID.randomUUID(),
                                "db.username",
                                "sa",
                                "db.password",
                                "",
                                "db.min.idle",
                                "0",
                                "db.max.pool.size",
                                "2"));
        DatabaseManager manager = new DatabaseManager(cfg);
        try (manager) {
            manager.database()
                    .useHandle(
                            handle ->
                                    handle.execute(
                                            """
                    CREATE TABLE users (id VARCHAR(64), name VARCHAR(255), job VARCHAR(255),
                    email VARCHAR(255), created_at VARCHAR(64))
                    """));
            UserRepository repo = UserRepository.create(manager.database());
            repo.insert("1", "QA", "Tester", "qa@example.com", "2026-01-01T00:00:00Z");
            assertThat(repo.findById("1"))
                    .isPresent()
                    .get()
                    .extracting("email")
                    .isEqualTo("qa@example.com");
            repo.deleteById("1");
            assertThat(repo.findById("1")).isEmpty();
        }
        assertThatThrownBy(manager::database).isInstanceOf(IllegalStateException.class);
    }
}
