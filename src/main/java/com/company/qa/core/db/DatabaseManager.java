package com.company.qa.core.db;

import com.company.qa.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

/** Explicitly owned pools for fixtures; the default pool is created only when requested. */
public final class DatabaseManager implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final Jdbi database;

    public DatabaseManager(DatabaseConfig cfg) {
        HikariConfig hikari = new HikariConfig();
        hikari.setDriverClassName(cfg.dbDriver());
        hikari.setJdbcUrl(cfg.dbUrl());
        hikari.setUsername(cfg.dbUsername());
        hikari.setPassword(cfg.dbPassword());
        hikari.setMaximumPoolSize(cfg.dbMaxPoolSize());
        hikari.setMinimumIdle(cfg.dbMinIdle());
        hikari.setConnectionTimeout(10000);
        dataSource = new HikariDataSource(hikari);
        database = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
    }

    public Jdbi database() {
        if (dataSource.isClosed()) throw new IllegalStateException("Database pool is closed");
        return database;
    }

    public static Jdbi jdbi() {
        return DefaultPool.INSTANCE.database();
    }

    public static void executeUpdate(String sql, Object... params) {
        jdbi().useHandle(handle -> handle.execute(sql, params));
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static final class DefaultPool {
        private static final DatabaseManager INSTANCE = new DatabaseManager(DatabaseConfig.get());

        static {
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(INSTANCE::close, "qa-database-cleanup"));
        }
    }
}
