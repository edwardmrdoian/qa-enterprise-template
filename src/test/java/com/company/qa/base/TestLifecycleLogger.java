package com.company.qa.base;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Writes START / PASSED / FAILED lines for every test into the log file.
 * Registered automatically through META-INF/services (extension autodetection is on).
 */
public class TestLifecycleLogger implements BeforeTestExecutionCallback, TestWatcher {

    private static final Logger log = LoggerFactory.getLogger("TEST");

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
