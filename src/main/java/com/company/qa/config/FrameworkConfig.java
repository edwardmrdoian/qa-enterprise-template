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
public interface FrameworkConfig extends Config {

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

    static FrameworkConfig get() {
        return ConfigCache.getOrCreate(FrameworkConfig.class);
    }
}
