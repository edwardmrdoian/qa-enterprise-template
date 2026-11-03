package com.company.qa.core.factory;

import com.company.qa.pages.BasePage;

/** Creates page objects: PageFactory.create(LoginPage.class).open() */
public final class PageFactory {

    private PageFactory() {}

    public static <T extends BasePage> T create(Class<T> pageType) {
        try {
            return pageType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot create page "
                            + pageType.getSimpleName()
                            + ". It needs a public no-arg constructor.",
                    e);
        }
    }
}
