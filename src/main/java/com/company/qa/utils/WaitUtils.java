package com.company.qa.utils;

import java.time.Duration;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

public final class WaitUtils {

    private WaitUtils() {}

    public static ConditionFactory poll() {
        return poll(Duration.ofSeconds(15));
    }

    public static ConditionFactory poll(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Polling timeout must be positive");
        }
        return Awaitility.await()
                .atMost(timeout)
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(500))
                .pollInSameThread();
    }
}
