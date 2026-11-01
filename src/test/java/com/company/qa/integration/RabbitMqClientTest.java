package com.company.qa.integration;

import com.company.qa.core.factory.ClientFactory;
import com.company.qa.core.messaging.ReceivedMessage;
import com.company.qa.core.messaging.RabbitMqClient;
import com.company.qa.models.UserRecord;
import com.company.qa.utils.TestDataFactory;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("RabbitMQ")
@Tag("integration")
@EnabledIfSystemProperty(named = "rabbit.enabled", matches = "true")
class RabbitMqClientTest {

    @Test
    @DisplayName("Publish JSON to a queue and read it back")
    void publishAndConsume() {
        String queue = "qa.test." + UUID.randomUUID();
        try (RabbitMqClient rabbit = ClientFactory.rabbitMq()) {
            rabbit.declareQueue(queue);
            UserRecord user = TestDataFactory.createRandomUser();

            rabbit.publish(queue, user);
            ReceivedMessage message = rabbit.waitForMessage(queue, Duration.ofSeconds(10));

            assertThat(message.as(UserRecord.class)).isEqualTo(user);
            assertThat(rabbit.messageCount(queue)).isZero();
            rabbit.deleteQueue(queue);
        }
    }

    @Test
    @DisplayName("Messages published through a direct exchange reach the bound queue with headers")
    void exchangeRouting() {
        String id = UUID.randomUUID().toString();
        String queue = "qa.queue." + id;
        String exchange = "qa.exchange." + id;
        try (RabbitMqClient rabbit = ClientFactory.rabbitMq()) {
            rabbit.declareExchange(exchange, com.rabbitmq.client.BuiltinExchangeType.DIRECT)
                    .declareQueue(queue)
                    .bindQueue(queue, exchange, "orders");

            rabbit.publish(exchange, "orders", Map.of("orderId", 42), Map.of("source", "qa"));

            ReceivedMessage message = rabbit.waitForMessage(queue,
                    m -> "qa".equals(m.headers().get("source")), Duration.ofSeconds(10));

            assertThat(message.routingKey()).isEqualTo("orders");
            assertThat(message.body()).contains("\"orderId\":42");
            rabbit.deleteQueue(queue).deleteExchange(exchange);
        }
    }
}
