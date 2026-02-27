package com.framework.core.config;

import com.framework.core.ai.healing.SelfHealingDriver;
import com.framework.core.driver.DriverManager;
import com.framework.core.interfaces.IDriver;
import com.framework.core.interfaces.ILocatorRepository;
import com.framework.core.interfaces.ISelfHealer;
import com.framework.utils.LoggerUtil;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

public abstract class AbstractBase {

    // ── Dependencies (initialized once in constructor) ──

    protected IDriver              driver;
    protected ILocatorRepository   locatorRepo;
    protected ISelfHealer          healer;
    protected String               platform;
    protected FluentWait<WebDriver> fluentWait;
    protected FluentWait<WebDriver> shortFluentWait;

    /**
     * Constructor — initializes all dependencies when a page object is created.
     *
     * Prerequisite: DriverManager.setDriver() must be called BEFORE creating page objects.
     * This is guaranteed because BaseTest.@BeforeMethod creates the driver first,
     * then Guice.injectMembers(this) creates page objects.
     */
    protected AbstractBase() {
        this.driver      = DriverManager.getDriver();
        this.locatorRepo = YamlLocatorRepository.getInstance();
        this.healer      = new SelfHealingDriver();
        this.platform    = ConfigManager.get("platform", "android");

        int timeoutSecs  = ConfigManager.getInt("fluent.wait.timeout", 10);
        int pollMillis   = ConfigManager.getInt("fluent.wait.polling", 500);
        int shortTimeout = ConfigManager.getInt("fluent.wait.short.timeout", 5);

        WebDriver rawDriver = (WebDriver) driver.getUnderlyingDriver();

        this.fluentWait = new FluentWait<>(rawDriver)
                .withTimeout(Duration.ofSeconds(timeoutSecs))
                .pollingEvery(Duration.ofMillis(pollMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class);

        this.shortFluentWait = new FluentWait<>(rawDriver)
                .withTimeout(Duration.ofSeconds(shortTimeout))
                .pollingEvery(Duration.ofMillis(pollMillis))
                .ignoring(NoSuchElementException.class);

        LoggerUtil.info("[Page] Initialized " + getPageName()
                + " [thread: " + Thread.currentThread().getName() + "]");
    }

    /**
     * Returns the page name used to resolve YAML locators.
     * Auto-derived from the concrete class name:
     *   LoginPage.java → "LoginPage" → reads LoginPage.yaml
     */
    public String getPageName() {
        return this.getClass().getSimpleName();
    }
}
