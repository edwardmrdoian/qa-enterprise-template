package com.company.qa.config;

import org.aeonbits.owner.Config;

public interface FrameworkConfig extends Config {

    @Key("env")
    String environment();

    @Key("base.url")
    String baseUrl();

    @Key("api.base.url")
    String apiBaseUrl();

    @Key("api.key")
    @DefaultValue("")
    String apiKey();

    @Key("browser")
    @DefaultValue("chrome")
    String browser();

    @Key("headless")
    @DefaultValue("true")
    boolean headless();

    @Key("timeout.seconds")
    @DefaultValue("10")
    int timeoutSeconds();

    @Key("api.connect.timeout.ms")
    int apiConnectTimeoutMs();

    @Key("api.read.timeout.ms")
    int apiReadTimeoutMs();

    static FrameworkConfig get() {
        return ConfigSupport.load(FrameworkConfig.class);
    }
}
