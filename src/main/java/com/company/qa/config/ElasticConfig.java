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
public interface ElasticConfig extends Config {

    @Key("elastic.url")
    @DefaultValue("http://localhost:9200")
    String url();

    @Key("elastic.username")
    @DefaultValue("")
    String username();

    @Key("elastic.password")
    @DefaultValue("")
    String password();

    static ElasticConfig get() {
        return ConfigCache.getOrCreate(ElasticConfig.class);
    }
}
