package com.company.qa.core.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts a browser per test, and on failure attaches a screenshot and a Playwright trace to Allure.
 * Open the trace zip at https://trace.playwright.dev or with: mvn exec:java -e -D
 * exec.mainClass=com.microsoft.playwright.CLI -D exec.args="show-trace file.zip"
 */
public class PlaywrightExtension
        implements BeforeEachCallback,
                AfterTestExecutionCallback,
                AfterEachCallback,
                ParameterResolver {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightExtension.class);
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(PlaywrightExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        PlaywrightManager.start();
    }

    // Runs right after the test body and BEFORE @AfterEach, so the page is still open.
    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) captureFailure(context);
    }

    private void captureFailure(ExtensionContext context) {
        context.getStore(NAMESPACE).put("captured", true);
        Path trace = Path.of("target", "traces", fileName(context) + ".zip");
        try {
            attachScreenshot();
        } catch (RuntimeException e) {
            log.warn("Cannot capture screenshot for {}", context.getDisplayName(), e);
        }
        try {
            PlaywrightManager.stopTracing(trace);
            if (Files.exists(trace)) {
                try (var input = Files.newInputStream(trace)) {
                    Allure.addAttachment("Playwright trace", "application/zip", input, "zip");
                }
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot capture trace for {}", context.getDisplayName(), e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            if (!Boolean.TRUE.equals(context.getStore(NAMESPACE).get("captured", Boolean.class))) {
                if (context.getExecutionException().isPresent()) captureFailure(context);
                else PlaywrightManager.stopTracing(null);
            }
        } finally {
            PlaywrightManager.stop();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        Class<?> type = parameter.getParameter().getType();
        return type == Page.class || type == BrowserContext.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == Page.class
                ? PlaywrightManager.page()
                : PlaywrightManager.context();
    }

    private void attachScreenshot() {
        byte[] png =
                PlaywrightManager.page().screenshot(new Page.ScreenshotOptions().setFullPage(true));
        Allure.addAttachment(
                "Screenshot on failure", "image/png", new ByteArrayInputStream(png), "png");
    }

    private String fileName(ExtensionContext context) {
        return (context.getRequiredTestClass().getSimpleName()
                        + "-"
                        + context.getRequiredTestMethod().getName()
                        + "-"
                        + java.util.UUID.randomUUID())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
