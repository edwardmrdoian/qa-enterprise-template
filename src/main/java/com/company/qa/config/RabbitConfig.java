package com.company.qa.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:config/${env}.properties",
    "classpath:config/qa.properties"
})
public interface RabbitConfig extends Config {

    @Key("rabbit.host")
    @DefaultValue("localhost")
    String host();

    @Key("rabbit.port")
    @DefaultValue("5672")
    int port();

    @Key("rabbit.username")
    @DefaultValue("guest")
    String username();

    @Key("rabbit.password")
    @DefaultValue("guest")
    String password();

    @Key("rabbit.vhost")
    @DefaultValue("/")
    String vhost();

    static RabbitConfig get() {
        return ConfigCache.getOrCreate(RabbitConfig.class);
    }
}
