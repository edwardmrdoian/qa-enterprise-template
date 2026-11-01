package com.company.qa.core.db;

import com.company.qa.config.DatabaseConfig;
import com.company.qa.models.UserRecord;
import com.company.qa.utils.Secrets;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final HikariDataSource DATA_SOURCE;
    private static final Jdbi JDBI_INSTANCE;

    static {
        DatabaseConfig cfg = DatabaseConfig.get();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(cfg.dbDriver());
        hikariConfig.setJdbcUrl(cfg.dbUrl());
        hikariConfig.setUsername(cfg.dbUsername());
        hikariConfig.setPassword(Secrets.get("db.password", cfg.dbPassword()));
        hikariConfig.setMaximumPoolSize(cfg.dbMaxPoolSize());
        hikariConfig.setMinimumIdle(cfg.dbMinIdle());
        hikariConfig.setPoolName("QA-Automation-Pool");

        DATA_SOURCE = new HikariDataSource(hikariConfig);
        JDBI_INSTANCE = Jdbi.create(DATA_SOURCE);
        JDBI_INSTANCE.installPlugin(new SqlObjectPlugin());
        JDBI_INSTANCE.registerRowMapper(ConstructorMapper.factory(UserRecord.class));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
                log.info("Closing HikariCP connection pool...");
                DATA_SOURCE.close();
            }
        }));
    }

    private DatabaseManager() {}

    public static Jdbi jdbi() {
        return JDBI_INSTANCE;
    }

    public static void executeUpdate(String sql, Object... params) {
        jdbi().useHandle(handle -> handle.execute(sql, params));
    }
}
