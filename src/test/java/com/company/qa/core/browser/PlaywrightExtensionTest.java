package com.company.qa.core.browser;

import static org.assertj.core.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.microsoft.playwright.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;

@Tag("framework")
@Tag("ui")
class PlaywrightExtensionTest {
    @Test
    void parameterizedFailuresProduceDistinctTracesAndCloseSessions() throws IOException {
        verifyFailureEvidence(BodyFailure.class, 2);
    }

    @Test
    void setupFailureStillProducesEvidenceAndClosesSession() throws IOException {
        verifyFailureEvidence(SetupFailure.class, 1);
    }

    @Test
    void teardownFailureStillProducesEvidenceAndClosesSession() throws IOException {
        verifyFailureEvidence(TeardownFailure.class, 1);
    }

    private static void verifyFailureEvidence(Class<?> fixture, int expectedFailures)
            throws IOException {
        Set<Path> previous = traces(fixture);
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(fixture))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .execute()
                .testEvents()
                .assertStatistics(
                        stats -> stats.started(expectedFailures).failed(expectedFailures));
        Set<Path> created = traces(fixture);
        created.removeAll(previous);
        assertThat(created).hasSize(expectedFailures);
        for (Path trace : created) {
            assertThat(Files.size(trace)).isPositive();
            try (var zip = new java.util.zip.ZipFile(trace.toFile())) {
                assertThat(zip.stream().anyMatch(entry -> entry.getName().endsWith(".trace")))
                        .isTrue();
            }
        }
        java.util.List<io.qameta.allure.model.Attachment> attachments = new java.util.ArrayList<>();
        io.qameta.allure.Allure.getLifecycle()
                .updateTestCase(result -> attachments.addAll(result.getAttachments()));
        assertThat(attachments.stream().filter(a -> a.getType().equals("image/png")).count())
                .isEqualTo(expectedFailures);
        assertThat(attachments.stream().filter(a -> a.getType().equals("application/zip")).count())
                .isEqualTo(expectedFailures);
        assertThatThrownBy(PlaywrightManager::page).isInstanceOf(IllegalStateException.class);
    }

    private static Set<Path> traces(Class<?> fixture) throws IOException {
        Path folder = Path.of("target", "traces");
        if (!Files.exists(folder)) return new java.util.HashSet<>();
        try (var files = Files.list(folder)) {
            return files.filter(
                            file ->
                                    file.getFileName()
                                            .toString()
                                            .startsWith(fixture.getSimpleName() + "-"))
                    .collect(Collectors.toSet());
        }
    }

    @ExtendWith(PlaywrightExtension.class)
    static class BodyFailure {
        @ParameterizedTest
        @ValueSource(ints = {1, 2})
        void failsWithEvidence(int invocation, Page page) {
            page.setContent("<h1>Expected failure " + invocation + "</h1>");
            throw new AssertionError("Intentional fixture failure");
        }
    }

    @ExtendWith(PlaywrightExtension.class)
    static class SetupFailure {
        @BeforeEach
        void setup(Page page) {
            page.setContent("<h1>Setup failure</h1>");
            throw new AssertionError("Intentional setup failure");
        }

        @Test
        void neverRuns() {}
    }

    @ExtendWith(PlaywrightExtension.class)
    static class TeardownFailure {
        @Test
        void succeeds(Page page) {
            page.setContent("<h1>Teardown failure</h1>");
        }

        @AfterEach
        void teardown() {
            throw new AssertionError("Intentional teardown failure");
        }
    }
}
