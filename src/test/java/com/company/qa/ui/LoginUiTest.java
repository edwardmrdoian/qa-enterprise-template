package com.company.qa.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.qa.base.BaseUiTest;
import com.company.qa.pages.LoginPage;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Feature("Login")
@Tag("ui")
@Tag("example")
@Tag("external")
class LoginUiTest extends BaseUiTest {

    @Test
    @Tag("smoke")
    @DisplayName("Valid credentials lead to the secure area")
    void validLogin() {
        LoginPage page = new LoginPage().open().loginAs("tomsmith", "SuperSecretPassword!");

        // flashMessage() waits for the new page, so the URL check afterwards is stable
        assertThat(page.flashMessage()).contains("You logged into a secure area!");
        assertThat(page.currentUrl()).endsWith("/secure");
    }

    @Test
    @Tag("regression")
    @DisplayName("Invalid credentials show an error")
    void invalidLogin() {
        LoginPage page = new LoginPage().open().loginAs("wrong", "wrong");

        assertThat(page.flashMessage()).contains("Your username is invalid!");
    }
}
