package com.company.qa.base;

import com.company.qa.config.FrameworkConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes START / PASSED / FAILED lines for every test into the log file. Registered automatically
 * through META-INF/services (extension autodetection is on).
 */
public class TestLifecycleLogger
        implements BeforeAllCallback, BeforeTestExecutionCallback, TestWatcher {

    private static final Logger log = LoggerFactory.getLogger("TEST");

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot()
                .getStore(ExtensionContext.Namespace.create(TestLifecycleLogger.class))
                .getOrComputeIfAbsent(
                        "environment",
                        key -> {
                            var cfg = FrameworkConfig.get();
                            var values = new Properties();
                            values.setProperty("Environment", cfg.environment());
                            values.setProperty("Browser", cfg.browser());
                            values.setProperty("Headless", Boolean.toString(cfg.headless()));
                            values.setProperty("Java", System.getProperty("java.version"));
                            values.setProperty("OS", System.getProperty("os.name"));
                            var directory =
                                    Path.of(
                                            System.getProperty(
                                                    "allure.results.directory",
                                                    "target/allure-results"));
                            try {
                                Files.createDirectories(directory);
                                try (var output =
                                        Files.newOutputStream(
                                                directory.resolve("environment.properties"))) {
                                    values.store(
                                            output,
                                            "Resolved runtime configuration (no credentials)");
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(
                                        "Cannot write Allure environment metadata", e);
                            }
                            return Boolean.TRUE;
                        },
                        Boolean.class);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        log.info("START   {}", name(context));
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        log.info("PASSED  {}", name(context));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        log.error("FAILED  {} -> {}", name(context), cause.toString());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        log.warn("ABORTED {} -> {}", name(context), cause.toString());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        log.info("SKIPPED {} ({})", name(context), reason.orElse("no reason"));
    }

    private String name(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "." + context.getDisplayName();
    }
}
