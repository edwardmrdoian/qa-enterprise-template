package com.company.qa.core.factory;

import com.company.qa.clients.UserApiClient;
import com.company.qa.core.messaging.RabbitMqClient;
import com.company.qa.core.search.ElasticClient;

/**
 * Convenience constructors for configured services and isolated mock endpoints. Tests may construct
 * clients directly when they need explicit dependencies.
 */
public final class ClientFactory {

    private ClientFactory() {}

    public static UserApiClient userApi() {
        return new UserApiClient();
    }

    public static UserApiClient userApi(String baseUrl) {
        return new UserApiClient(baseUrl);
    }

    /** New connection per call (channels are not thread-safe). Close it with try-with-resources. */
    public static RabbitMqClient rabbitMq() {
        return new RabbitMqClient();
    }

    public static ElasticClient elastic() {
        return new ElasticClient();
    }
}
