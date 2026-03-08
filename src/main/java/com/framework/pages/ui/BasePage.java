package com.framework.pages.ui;
import com.framework.core.config.AbstractBase;
import com.framework.core.config.ConfigManager;
import com.framework.core.config.YamlLocatorRepository;
import com.framework.utils.LoggerUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.List;
import java.util.function.Function;

/**
 * BasePage - Abstract base for all Page Objects.
 *
 * All dependencies (driver, locatorRepo, healer, fluentWait) are initialized
 * once in the AbstractBase constructor — no manual init calls needed anywhere.
 *
 * Parameterized Locators (varargs):
 * Every element method accepts String... replacements as the last parameter.
 * If YAML locators contain %s placeholders, they get replaced sequentially at runtime.
 * If no placeholders exist, the method works exactly as before — no impact.
 *
 * YAML:
 *   menuItem:
 *     android: xpath:://*[@text='%s'] | accessibilityId::%s
 *
 * Java:
 *   tap("menuItem", "Settings")    -> resolves to: xpath:://*[@text='Settings']
 *   tap("loginButton")             -> no placeholders, works as before
 */
public abstract class BasePage extends AbstractBase {

    // ── Locator resolution ────────────────────────────────────

    /**
     * Returns the primary By locator for an element, with optional placeholder replacement.
     *
     * @param elementName  YAML key
     * @param replacements values to substitute for %s placeholders (optional)
     */
    protected By loc(String elementName, String... replacements) {
        List<YamlLocatorRepository.LocatorStrategy> strategies =
                locatorRepo.getAllStrategies(getPageName(), elementName, platform);
        if (strategies.isEmpty()) {
            return locatorRepo.getLocator(getPageName(), elementName, platform);
        }
        return strategies.get(0).resolve(replacements).toBy(elementName);
    }

    // ── Core element finder ───────────────────────────────────

    /**
     * Finds an element using all YAML OR strategies, with optional placeholder replacement.
     * Tries each strategy left-to-right with FluentWait polling.
     * If all fail, invokes self-healing.
     *
     * @param elementName  YAML key (e.g., "loginButton")
     * @param replacements values to substitute for %s placeholders in locator values (optional)
     * @return the found WebElement
     */
    protected WebElement findElement(String elementName, String... replacements) {
        List<YamlLocatorRepository.LocatorStrategy> rawStrategies =
                locatorRepo.getAllStrategies(getPageName(), elementName, platform);

        // Resolve placeholders in every strategy
        List<YamlLocatorRepository.LocatorStrategy> strategies = rawStrategies.stream()
                .map(s -> s.resolve(replacements))
                .toList();

        // Step 1: Try all YAML OR strategies with FluentWait (polling + timeout)
        try {
            return fluentWait.until(d -> {
                for (YamlLocatorRepository.LocatorStrategy strategy : strategies) {
                    try {
                        return driver.findElement(strategy.toBy(elementName));
                    } catch (NoSuchElementException ignored) {
                    }
                }
                throw new NoSuchElementException(
                        "None of the " + strategies.size() + " YAML strategies found element: " + elementName);
            });
        } catch (Exception e) {
            // Step 2: All YAML strategies failed
            if (!ConfigManager.getBoolean("self.healing.enabled", true)) {
                throw new NoSuchElementException(
                        "Element '" + elementName + "' not found. Self-healing is disabled (self.healing.enabled=false).");
            }
            LoggerUtil.warn("[Page] All YAML strategies exhausted for '" + elementName
                    + "'. Invoking self-healing...");
        }

        // Step 2: All YAML strategies failed — invoke self-healing with ALL resolved locators
        List<By> allFailedLocators = strategies.stream()
                .map(s -> s.toBy(elementName))
                .toList();
        return healer.heal(allFailedLocators, elementName, driver);
    }

    // ── Element interactions ──────────────────────────────────

    /**
     * Taps (clicks) an element.
     * @param elementName  YAML key
     * @param replacements values for %s placeholders — optional
     */
    public void tap(String elementName, String... replacements) {
        LoggerUtil.debug("[Page] Tapping: " + elementName);
        waitForClickable(elementName, replacements).click();
    }

    /**
     * Enters text into an element (clears first).
     * @param elementName  YAML key
     * @param text         text to type
     * @param replacements values for %s placeholders — optional
     */
    protected void enterText(String elementName, String text, String... replacements) {
        LoggerUtil.debug("[Page] Entering text in: " + elementName + " → " + text);
        WebElement element = waitForVisible(elementName, replacements);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Gets text from an element.
     * @param elementName  YAML key
     * @param replacements values for %s placeholders — optional
     */
    protected String getText(String elementName, String... replacements) {
        return waitForVisible(elementName, replacements).getText();
    }

    /**
     * Checks if an element is visible on screen.
     * @param elementName  YAML key
     * @param replacements values for %s placeholders — optional
     */
    public boolean isVisible(String elementName, String... replacements) {
        try {
            return findElement(elementName, replacements).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if an element exists in the DOM (may not be visible).
     * @param elementName  YAML key
     * @param replacements values for %s placeholders — optional
     */
    protected boolean isPresent(String elementName, String... replacements) {
        try {
            findElement(elementName, replacements);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── FluentWait strategies ─────────────────────────────────

    protected WebElement waitForVisible(String elementName, String... replacements) {
        return findElement(elementName, replacements);
    }

    protected WebElement waitForClickable(String elementName, String... replacements) {
        WebElement element = findElement(elementName, replacements);
        fluentWait.until(ExpectedConditions.elementToBeClickable(element));
        return element;
    }

    protected void waitForInvisible(String elementName, String... replacements) {
        fluentWait.until(ExpectedConditions.invisibilityOfElementLocated(loc(elementName, replacements)));
    }

    protected <V> V waitFor(Function<WebDriver, V> condition, String description) {
        LoggerUtil.debug("[Page] Waiting for: " + description);
        return fluentWait.until(condition);
    }

    // ── Scroll ────────────────────────────────────────────────

    protected void scrollDown() {
        executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
    }

    protected void scrollUp() {
        executeScript("mobile: scroll", java.util.Map.of("direction", "up"));
    }

    protected void scrollToElement(String elementName, String... replacements) {
        executeScript("arguments[0].scrollIntoView(true);", findElement(elementName, replacements));
    }

    private void executeScript(String script, Object... args) {
        ((JavascriptExecutor) driver.getUnderlyingDriver()).executeScript(script, args);
    }
}
