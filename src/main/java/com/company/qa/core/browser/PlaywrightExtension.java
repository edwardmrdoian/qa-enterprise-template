package com.company.qa.core.browser;

import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts a browser per test, and on failure attaches a screenshot and a Playwright trace to Allure.
 * Open the trace zip at https://trace.playwright.dev or with: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="show-trace file.zip"
 */
public class PlaywrightExtension implements BeforeEachCallback, AfterTestExecutionCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        PlaywrightManager.start();
    }

    // Runs right after the test body and BEFORE @AfterEach, so the page is still open.
    @Override
    public void afterTestExecution(ExtensionContext context) {
        boolean failed = context.getExecutionException().isPresent();
        Path trace = failed
                ? Path.of("target", "traces", fileName(context) + ".zip")
                : null;
        try {
            if (failed) attachScreenshot();
        } catch (RuntimeException ignored) {
            // never hide the real test failure
        }
        try {
            PlaywrightManager.stopTracing(trace);
            if (trace != null && Files.exists(trace)) {
                Allure.addAttachment("Playwright trace", "application/zip",
                        Files.newInputStream(trace), "zip");
            }
        } catch (IOException | RuntimeException ignored) {
            // never hide the real test failure
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        PlaywrightManager.stop();
    }

    private void attachScreenshot() {
        byte[] png = PlaywrightManager.page().screenshot(new Page.ScreenshotOptions().setFullPage(true));
        Allure.addAttachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png), "png");
    }

    private String fileName(ExtensionContext context) {
        return (context.getRequiredTestClass().getSimpleName() + "-" + context.getRequiredTestMethod().getName())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
