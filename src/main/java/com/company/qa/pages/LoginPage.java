package com.company.qa.pages;

import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

public class LoginPage extends BasePage {
    public LoginPage() {}

    public LoginPage(Page page) {
        super(page);
    }

    private static final String USERNAME = "#username";
    private static final String PASSWORD = "#password";
    private static final String SUBMIT = "button[type='submit']";
    private static final String FLASH = "#flash";

    @Step("Open login page")
    public LoginPage open() {
        page.navigate("/login"); // relative to base.url (set on the browser context)
        return this;
    }

    public LoginPage loginAs(String username, String password) {
        Allure.step(
                "Log in",
                () -> {
                    locator(USERNAME).fill(username);
                    locator(PASSWORD).fill(password);
                    locator(SUBMIT).click();
                });
        return this;
    }

    /** Waits for the flash banner to appear, so it also doubles as "page finished loading". */
    public String flashMessage() {
        return locator(FLASH).innerText();
    }
}
