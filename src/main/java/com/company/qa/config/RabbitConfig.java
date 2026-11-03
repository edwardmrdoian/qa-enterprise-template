package com.company.qa.config;

import org.aeonbits.owner.Config;

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
        return ConfigSupport.load(RabbitConfig.class);
    }
}
