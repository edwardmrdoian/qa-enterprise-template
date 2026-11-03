package com.company.qa.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("framework")
class TestDataFactoryTest {
    @Test
    void explicitNullSupportsNegativeTestCases() {
        assertThat(TestDataFactory.user().seed(42).name(null).email(null).build())
                .satisfies(
                        user -> {
                            assertThat(user.name()).isNull();
                            assertThat(user.email()).isNull();
                            assertThat(user.job()).isNotBlank();
                        });
    }

    @Test
    void recordedSeedReproducesTheSameUser() {
        assertThat(TestDataFactory.user().seed(42).build())
                .isEqualTo(TestDataFactory.user().seed(42).build());
    }

    @Test
    void explicitFieldsOverrideGeneratedDefaults() {
        var user =
                TestDataFactory.user()
                        .seed(42)
                        .name("QA Engineer")
                        .job("Tester")
                        .email("qa@example.com")
                        .id("123")
                        .build();
        assertThat(user.name()).isEqualTo("QA Engineer");
        assertThat(user.email()).isEqualTo("qa@example.com");
        assertThat(user.id()).isEqualTo("123");
    }
}
