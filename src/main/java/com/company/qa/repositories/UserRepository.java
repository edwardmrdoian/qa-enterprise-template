package com.company.qa.repositories;

import com.company.qa.core.db.DatabaseManager;
import com.company.qa.models.UserRecord;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

public interface UserRepository {

    @SqlUpdate("INSERT INTO users (id, name, job, email, created_at) VALUES (:id, :name, :job, :email, :createdAt)")
    void insert(@Bind("id") String id, @Bind("name") String name, @Bind("job") String job,
                @Bind("email") String email, @Bind("createdAt") String createdAt);

    @SqlQuery("SELECT id, name, job, email, created_at FROM users WHERE id = :id")
    Optional<UserRecord> findById(@Bind("id") String id);

    @SqlQuery("SELECT COUNT(*) FROM users WHERE email = :email")
    int countByEmail(@Bind("email") String email);

    @SqlUpdate("DELETE FROM users WHERE id = :id")
    void deleteById(@Bind("id") String id);

    static UserRepository create() {
        return DatabaseManager.jdbi().onDemand(UserRepository.class);
    }
}
