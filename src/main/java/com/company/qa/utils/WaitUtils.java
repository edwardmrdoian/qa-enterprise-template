package com.company.qa.utils;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;

public final class WaitUtils {

    private WaitUtils() {}

    public static ConditionFactory poll() {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .pollInSameThread()
                .ignoreExceptions();
    }
}
