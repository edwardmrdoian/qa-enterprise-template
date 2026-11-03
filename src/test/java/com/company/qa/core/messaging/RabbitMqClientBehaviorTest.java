package com.company.qa.core.messaging;

import static org.assertj.core.api.Assertions.*;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("framework")
class RabbitMqClientBehaviorTest {
    @Test
    void timeoutRequeuesUnmatchedDelivery() {
        var deliveries = new ArrayDeque<>(List.of(message(1, "other")));
        List<Long> requeued = new ArrayList<>();
        Channel channel =
                proxy(
                        Channel.class,
                        (object, method, args) ->
                                switch (method.getName()) {
                                    case "basicGet" -> deliveries.poll();
                                    case "basicNack" -> {
                                        requeued.add((Long) args[0]);
                                        yield null;
                                    }
                                    default ->
                                            throw new UnsupportedOperationException(
                                                    method.getName());
                                });
        RabbitMqClient client = new RabbitMqClient(null, channel);
        assertThatThrownBy(() -> client.waitForMessage("test", m -> false, Duration.ofSeconds(1)))
                .isInstanceOf(org.awaitility.core.ConditionTimeoutException.class);
        assertThat(requeued).containsExactly(1L);
    }

    @Test
    void requeueFailureDoesNotReplaceMatcherFailure() {
        Channel channel =
                proxy(
                        Channel.class,
                        (object, method, args) ->
                                switch (method.getName()) {
                                    case "basicGet" -> message(1, "other");
                                    case "basicNack" -> throw new IOException("requeue failure");
                                    default ->
                                            throw new UnsupportedOperationException(
                                                    method.getName());
                                });
        assertThatThrownBy(
                        () ->
                                new RabbitMqClient(null, channel)
                                        .waitForMessage(
                                                "test",
                                                m -> {
                                                    throw new IllegalArgumentException(
                                                            "matcher failure");
                                                },
                                                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("matcher failure")
                .satisfies(
                        error ->
                                assertThat(error.getSuppressed())
                                        .hasSize(1)
                                        .allMatch(MessagingException.class::isInstance));
    }

    @Test
    void laterMatchingMessageIsFoundAndEarlierMessageIsRequeued() {
        var deliveries = new ArrayDeque<>(List.of(message(1, "other"), message(2, "wanted")));
        List<Long> acked = new ArrayList<>();
        List<Long> requeued = new ArrayList<>();
        Channel channel =
                proxy(
                        Channel.class,
                        (object, method, args) ->
                                switch (method.getName()) {
                                    case "basicGet" -> deliveries.poll();
                                    case "basicAck" -> {
                                        acked.add((Long) args[0]);
                                        yield null;
                                    }
                                    case "basicNack" -> {
                                        requeued.add((Long) args[0]);
                                        yield null;
                                    }
                                    default ->
                                            throw new UnsupportedOperationException(
                                                    method.getName());
                                });
        RabbitMqClient client = new RabbitMqClient(null, channel);
        assertThat(
                        client.waitForMessage(
                                        "test",
                                        m -> m.body().equals("wanted"),
                                        Duration.ofSeconds(3))
                                .body())
                .isEqualTo("wanted");
        assertThat(acked).containsExactly(2L);
        assertThat(requeued).containsExactly(1L);
    }

    @Test
    void matcherFailureStillRequeuesTheUnackedDelivery() {
        List<Long> requeued = new ArrayList<>();
        Channel channel =
                proxy(
                        Channel.class,
                        (object, method, args) ->
                                switch (method.getName()) {
                                    case "basicGet" -> message(1, "other");
                                    case "basicNack" -> {
                                        requeued.add((Long) args[0]);
                                        yield null;
                                    }
                                    default ->
                                            throw new UnsupportedOperationException(
                                                    method.getName());
                                });
        RabbitMqClient client = new RabbitMqClient(null, channel);
        assertThatThrownBy(
                        () ->
                                client.waitForMessage(
                                        "test",
                                        m -> {
                                            throw new IllegalArgumentException("bad matcher");
                                        },
                                        Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(requeued).containsExactly(1L);
    }

    @Test
    void connectionIsClosedEvenIfChannelCloseFails() {
        List<String> closed = new ArrayList<>();
        Channel channel =
                proxy(
                        Channel.class,
                        (object, method, args) -> {
                            if (method.getName().equals("isOpen")) return true;
                            if (method.getName().equals("close"))
                                throw new IOException("channel failure");
                            throw new UnsupportedOperationException(method.getName());
                        });
        Connection connection =
                proxy(
                        Connection.class,
                        (object, method, args) -> {
                            if (method.getName().equals("isOpen")) return true;
                            if (method.getName().equals("close")) {
                                closed.add("connection");
                                return null;
                            }
                            throw new UnsupportedOperationException(method.getName());
                        });
        assertThatThrownBy(() -> new RabbitMqClient(connection, channel).close())
                .isInstanceOf(MessagingException.class)
                .hasCauseInstanceOf(IOException.class);
        assertThat(closed).containsExactly("connection");
    }

    private static GetResponse message(long tag, String body) {
        return new GetResponse(
                new Envelope(tag, false, "", "test"),
                new AMQP.BasicProperties.Builder().build(),
                body.getBytes(StandardCharsets.UTF_8),
                0);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(
                Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
