package com.company.qa.core.messaging;

import com.company.qa.config.RabbitConfig;
import com.company.qa.utils.JsonUtils;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.awaitility.Awaitility;

/**
 * Thin RabbitMQ helper for tests. A Channel is NOT thread-safe, so create one client per test (see
 * ClientFactory) and close it with try-with-resources.
 */
public class RabbitMqClient implements AutoCloseable {

    private final Connection connection;
    private final Channel channel;

    public RabbitMqClient() {
        this(RabbitConfig.get());
    }

    public RabbitMqClient(RabbitConfig cfg) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.host());
        factory.setPort(cfg.port());
        factory.setUsername(cfg.username());
        factory.setPassword(cfg.password());
        factory.setVirtualHost(cfg.vhost());
        factory.setConnectionTimeout(10_000);
        try {
            this.connection = factory.newConnection("qa-automation");
            this.channel = openChannel(connection);
        } catch (IOException | TimeoutException e) {
            throw new MessagingException(
                    "Cannot connect to RabbitMQ at " + cfg.host() + ":" + cfg.port(), e);
        }
    }

    RabbitMqClient(Connection connection, Channel channel) {
        this.connection = connection;
        this.channel = channel;
    }

    private static Channel openChannel(Connection connection) throws IOException {
        try {
            Channel channel = connection.createChannel();
            if (channel == null) throw new IOException("RabbitMQ channel limit reached");
            return channel;
        } catch (IOException e) {
            try {
                connection.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    // ---------- topology ----------

    @Step("Declare queue {queue}")
    public RabbitMqClient declareQueue(String queue) {
        return declareQueue(queue, false, false, null);
    }

    public RabbitMqClient declareQueue(
            String queue, boolean durable, boolean autoDelete, Map<String, Object> args) {
        return run(
                "declare queue " + queue,
                () -> channel.queueDeclare(queue, durable, false, autoDelete, args));
    }

    @Step("Declare exchange {exchange}")
    public RabbitMqClient declareExchange(String exchange, BuiltinExchangeType type) {
        return run(
                "declare exchange " + exchange,
                () -> channel.exchangeDeclare(exchange, type, false));
    }

    @Step("Bind queue {queue} to exchange {exchange} with key {routingKey}")
    public RabbitMqClient bindQueue(String queue, String exchange, String routingKey) {
        return run("bind queue " + queue, () -> channel.queueBind(queue, exchange, routingKey));
    }

    @Step("Purge queue {queue}")
    public RabbitMqClient purgeQueue(String queue) {
        return run("purge queue " + queue, () -> channel.queuePurge(queue));
    }

    @Step("Delete queue {queue}")
    public RabbitMqClient deleteQueue(String queue) {
        return run("delete queue " + queue, () -> channel.queueDelete(queue));
    }

    @Step("Delete exchange {exchange}")
    public RabbitMqClient deleteExchange(String exchange) {
        return run("delete exchange " + exchange, () -> channel.exchangeDelete(exchange));
    }

    // ---------- publish ----------

    /** Publishes to the default exchange, so routing key = queue name. */
    public RabbitMqClient publish(String queue, Object body) {
        return publish("", queue, body, Map.of());
    }

    public RabbitMqClient publish(
            String exchange, String routingKey, Object body, Map<String, Object> headers) {
        AMQP.BasicProperties props =
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .headers(headers)
                        .build();
        byte[] payload = JsonUtils.toJson(body).getBytes(StandardCharsets.UTF_8);
        return Allure.step(
                "Publish message",
                () ->
                        run(
                                "publish to " + exchange + "/" + routingKey,
                                () -> channel.basicPublish(exchange, routingKey, props, payload)));
    }

    // ---------- consume ----------

    /** Fetches one message right now (auto-acked). Empty if the queue is empty. */
    @Step("Get message from queue {queue}")
    public Optional<ReceivedMessage> getMessage(String queue) {
        try {
            GetResponse response = channel.basicGet(queue, true);
            return Optional.ofNullable(response).map(this::toReceived);
        } catch (IOException e) {
            throw new MessagingException("Cannot get message from " + queue, e);
        }
    }

    /** Waits until any message arrives. */
    public ReceivedMessage waitForMessage(String queue, Duration timeout) {
        return waitForMessage(queue, m -> true, timeout);
    }

    /**
     * Waits for a message matching the predicate. Non-matching messages are returned to the queue.
     */
    @Step("Wait for message in queue {queue}")
    public ReceivedMessage waitForMessage(
            String queue, Predicate<ReceivedMessage> matcher, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Message timeout must be positive");
        }
        java.util.Objects.requireNonNull(matcher, "matcher");
        AtomicReference<ReceivedMessage> found = new AtomicReference<>();
        List<Long> pending = new ArrayList<>();
        Throwable primaryFailure = null;
        try {
            Awaitility.await("message in queue " + queue)
                    .atMost(timeout)
                    .pollDelay(Duration.ZERO)
                    .pollInterval(Duration.ofMillis(250))
                    .pollInSameThread()
                    .until(
                            () -> {
                                try {
                                    GetResponse response = channel.basicGet(queue, false);
                                    if (response == null) return false;
                                    long tag = response.getEnvelope().getDeliveryTag();
                                    pending.add(tag);
                                    ReceivedMessage message = toReceived(response);
                                    if (matcher.test(message)) {
                                        channel.basicAck(tag, false);
                                        pending.remove(tag);
                                        found.set(message);
                                        return true;
                                    }
                                    return false;
                                } catch (IOException e) {
                                    throw new MessagingException("Cannot consume from " + queue, e);
                                }
                            });
            return found.get();
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            // Keep unmatched deliveries unacked while scanning so a requeued head cannot starve
            // later matches.
            MessagingException cleanupFailure = null;
            for (long tag : pending) {
                try {
                    channel.basicNack(tag, false, true);
                } catch (IOException e) {
                    if (cleanupFailure == null)
                        cleanupFailure =
                                new MessagingException("Cannot requeue unmatched delivery", e);
                    else cleanupFailure.addSuppressed(e);
                }
            }
            if (cleanupFailure != null) {
                if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            }
        }
    }

    @Step("Count messages in queue {queue}")
    public long messageCount(String queue) {
        try {
            return channel.messageCount(queue);
        } catch (IOException e) {
            throw new MessagingException("Cannot count messages in " + queue, e);
        }
    }

    @Override
    public void close() {
        Exception failure = null;
        try {
            if (channel.isOpen()) channel.close();
        } catch (IOException | TimeoutException e) {
            failure = e;
        } finally {
            try {
                if (connection.isOpen()) connection.close();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) throw new MessagingException("Cannot close RabbitMQ client", failure);
    }

    // ---------- internals ----------

    private ReceivedMessage toReceived(GetResponse response) {
        Map<String, Object> headers = new HashMap<>();
        if (response.getProps().getHeaders() != null) {
            response.getProps()
                    .getHeaders()
                    .forEach((k, v) -> headers.put(k, v == null ? null : v.toString()));
        }
        return new ReceivedMessage(
                new String(response.getBody(), StandardCharsets.UTF_8),
                headers,
                response.getEnvelope().getExchange(),
                response.getEnvelope().getRoutingKey());
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private RabbitMqClient run(String description, IoAction action) {
        try {
            action.run();
            return this;
        } catch (IOException e) {
            throw new MessagingException("RabbitMQ failed to " + description, e);
        }
    }
}
