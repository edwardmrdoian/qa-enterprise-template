package com.company.qa.config;

import org.aeonbits.owner.Config;

public interface DatabaseConfig extends Config {

    @Key("db.driver")
    String dbDriver();

    @Key("db.url")
    String dbUrl();

    @Key("db.username")
    String dbUsername();

    @Key("db.password")
    @DefaultValue("")
    String dbPassword();

    @Key("db.max.pool.size")
    @DefaultValue("10")
    int dbMaxPoolSize();

    @Key("db.min.idle")
    @DefaultValue("2")
    int dbMinIdle();

    static DatabaseConfig get() {
        return ConfigSupport.load(DatabaseConfig.class);
    }
}
