package com.company.qa.core.factory;

import com.company.qa.clients.UserApiClient;
import com.company.qa.core.messaging.RabbitMqClient;
import com.company.qa.core.search.ElasticClient;

/**
 * Single place to obtain service clients. Tests never call "new" on infrastructure classes,
 * so swapping implementations (mock, container, remote) happens here only.
 */
public final class ClientFactory {

    private ClientFactory() {}

    public static UserApiClient userApi() {
        return new UserApiClient();
    }

    /** New connection per call (channels are not thread-safe). Close it with try-with-resources. */
    public static RabbitMqClient rabbitMq() {
        return new RabbitMqClient();
    }

    public static ElasticClient elastic() {
        return new ElasticClient();
    }
}
