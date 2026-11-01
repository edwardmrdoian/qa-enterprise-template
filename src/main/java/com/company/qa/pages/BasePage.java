package com.company.qa.pages;

import com.company.qa.core.browser.PlaywrightManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Playwright auto-waits for elements to be visible, enabled and stable before acting,
 * so page objects need no manual wait code.
 */
public abstract class BasePage {

    protected final Page page = PlaywrightManager.page();

    protected Locator locator(String selector) {
        return page.locator(selector);
    }

    public String currentUrl() {
        return page.url();
    }
}
