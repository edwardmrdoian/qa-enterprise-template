package com.company.qa.config;

import org.aeonbits.owner.Config;

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
        return ConfigSupport.load(ElasticConfig.class);
    }
}
