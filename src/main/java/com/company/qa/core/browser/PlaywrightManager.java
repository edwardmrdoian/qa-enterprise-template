package com.company.qa.core.browser;

import com.company.qa.config.FrameworkConfig;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One isolated browser session per test thread. Playwright objects are not thread-safe, so each
 * parallel test gets its own Playwright, Browser, Context and Page.
 */
public final class PlaywrightManager {

    private record Session(
            Playwright playwright, Browser browser, BrowserContext context, Page page) {}

    private static final ThreadLocal<Session> SESSION = new ThreadLocal<>();

    private PlaywrightManager() {}

    public static void start() {
        start(FrameworkConfig.get().baseUrl());
    }

    public static void start(String baseUrl) {
        if (SESSION.get() != null)
            throw new IllegalStateException("A browser session is already active on this thread");
        FrameworkConfig cfg = FrameworkConfig.get();
        Playwright playwright = Playwright.create();
        try {
            BrowserType type =
                    switch (cfg.browser().toLowerCase()) {
                        case "chrome", "chromium" -> playwright.chromium();
                        case "firefox" -> playwright.firefox();
                        case "webkit", "safari" -> playwright.webkit();
                        default ->
                                throw new IllegalArgumentException(
                                        "Browser not supported: " + cfg.browser());
                    };
            Browser browser =
                    type.launch(new BrowserType.LaunchOptions().setHeadless(cfg.headless()));
            BrowserContext context =
                    browser.newContext(
                            new Browser.NewContextOptions()
                                    .setViewportSize(1920, 1080)
                                    .setBaseURL(baseUrl));
            context.setDefaultTimeout(cfg.timeoutSeconds() * 1000.0);
            context.tracing()
                    .start(
                            new Tracing.StartOptions()
                                    .setScreenshots(true)
                                    .setSnapshots(true)
                                    .setSources(true));
            SESSION.set(new Session(playwright, browser, context, context.newPage()));
        } catch (RuntimeException e) {
            playwright.close();
            throw e;
        }
    }

    public static Page page() {
        Session session = SESSION.get();
        if (session == null) {
            throw new IllegalStateException(
                    "Browser session not started. UI tests must extend BaseUiTest (or use PlaywrightExtension).");
        }

        return session.page();
    }

    public static BrowserContext context() {
        Session session = SESSION.get();
        if (session == null) throw new IllegalStateException("Browser session not started");
        return session.context();
    }

    /** Stops tracing. A non-null path saves the trace zip; null discards it. */
    public static void stopTracing(Path outputZip) {
        Session session = SESSION.get();
        if (session == null) return;
        Tracing.StopOptions options = new Tracing.StopOptions();
        if (outputZip != null) {
            try {
                Files.createDirectories(outputZip.getParent());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            options.setPath(outputZip);
        }
        session.context().tracing().stop(options);
    }

    public static void stop() {
        Session session = SESSION.get();
        if (session == null) return;
        try {
            session.context().close();
        } finally {
            try {
                session.browser().close();
            } finally {
                try {
                    session.playwright().close();
                } finally {
                    SESSION.remove();
                }
            }
        }
    }
}
