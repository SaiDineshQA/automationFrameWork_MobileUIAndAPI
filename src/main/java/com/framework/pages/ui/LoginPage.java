package com.framework.pages.ui;

/**
 * LoginPage - Zero hardcoded locators. All come from LoginPage.yaml.
 * Injected via @Inject in test classes. AbstractBase constructor
 * initializes driver/locatorRepo/fluentWait automatically.
 */
public class LoginPage extends BasePage {

    // Element name constants — match keys in LoginPage.yaml exactly
    private static final String USERNAME_FIELD     = "usernameField";
    private static final String PASSWORD_FIELD     = "passwordField";
    private static final String LOGIN_BUTTON       = "loginButton";
    private static final String ERROR_MESSAGE      = "errorMessage";
    private static final String FORGOT_PASSWORD    = "forgotPasswordLink";

    public static final String OK_BUTTON = "okayButton";

    public static final String FIND_STORE_BUTTON = "findStoreButton";


    // ── Actions ───────────────────────────────────────────────

    public LoginPage typeUsername(String username) {
        enterText(USERNAME_FIELD, username);
        return this;
    }

    public LoginPage typePassword(String password) {
        enterText(PASSWORD_FIELD, password);
        return this;
    }

    public void tapLogin() {
        tap(LOGIN_BUTTON);
    }

    public void tapForgotPassword() {
        tap(FORGOT_PASSWORD);
    }

    /** Fluent login: chains username, password, tap */
    public void loginWith(String username, String password) {
        typeUsername(username)
            .typePassword(password)
            .tapLogin();
    }

    // ── Assertions helpers ────────────────────────────────────

    public boolean isErrorVisible() {
        return isVisible(ERROR_MESSAGE);
    }

    public String getErrorText() {
        return getText(ERROR_MESSAGE);
    }

}
