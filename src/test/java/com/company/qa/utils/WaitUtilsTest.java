package com.company.qa.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("framework")
class WaitUtilsTest {
    @Test
    void unexpectedErrorsAreNotConvertedToTimeouts() {
        assertThatThrownBy(
                        () ->
                                WaitUtils.poll(Duration.ofSeconds(2))
                                        .until(
                                                () -> {
                                                    throw new IllegalArgumentException(
                                                            "invalid query");
                                                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid query");
    }

    @Test
    void rejectsInvalidTimeout() {
        assertThatThrownBy(() -> WaitUtils.poll(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
