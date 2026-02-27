package com.framework.pages.ui;
import com.framework.core.config.AbstractBase;
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
 */
public abstract class BasePage extends AbstractBase {

    // ── Locator resolution ────────────────────────────────────

    protected By loc(String elementName) {
        return locatorRepo.getLocator(getPageName(), elementName, platform);
    }

    // ── Element interactions with FluentWait ──────────────────

    protected WebElement findElement(String elementName) {
        List<YamlLocatorRepository.LocatorStrategy> yamlStrategies =
                locatorRepo.getAllStrategies(getPageName(), elementName, platform);

        // Step 1: Try all YAML OR strategies with FluentWait (polling + timeout)
        try {
            return fluentWait.until(d -> {
                for (YamlLocatorRepository.LocatorStrategy strategy : yamlStrategies) {
                    try {
                        return driver.findElement(strategy.toBy(elementName));
                    } catch (NoSuchElementException ignored) {
                    }
                }
                throw new NoSuchElementException(
                        "None of the " + yamlStrategies.size() + " YAML strategies found element: " + elementName);
            });
        } catch (Exception e) {
            LoggerUtil.warn("[Page] All YAML strategies exhausted for '" + elementName
                    + "'. Invoking self-healing...");
        }

        // Step 2: All YAML strategies failed — invoke self-healing with ALL locators
      /*  List<By> allFailedLocators = new java.util.ArrayList<>();
        for (YamlLocatorRepository.LocatorStrategy strategy : yamlStrategies) {
            allFailedLocators.add(strategy.toBy(elementName));
        }
        return healer.heal(allFailedLocators, elementName, driver);*/
        return null;
    }

    public void tap(String elementName) {
        LoggerUtil.debug("[Page] Tapping: " + elementName);
        waitForClickable(elementName).click();
    }

    protected void enterText(String elementName, String text) {
        LoggerUtil.debug("[Page] Entering text in: " + elementName + " → " + text);
        WebElement element = waitForVisible(elementName);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(String elementName) {
        return waitForVisible(elementName).getText();
    }

    public boolean isVisible(String elementName) {
        try {
            return findElement(elementName).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isPresent(String elementName) {
        try {
            findElement(elementName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── FluentWait strategies ─────────────────────────────────

    protected WebElement waitForVisible(String elementName) {
        return findElement(elementName);
    }

    protected WebElement waitForClickable(String elementName) {
        WebElement element = findElement(elementName);
        fluentWait.until(ExpectedConditions.elementToBeClickable(element));
        return element;
    }

    protected void waitForInvisible(String elementName) {
        fluentWait.until(ExpectedConditions.invisibilityOfElementLocated(loc(elementName)));
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

    protected void scrollToElement(String elementName) {
        executeScript("arguments[0].scrollIntoView(true);", findElement(elementName));
    }

    private void executeScript(String script, Object... args) {
        ((JavascriptExecutor) driver.getUnderlyingDriver()).executeScript(script, args);
    }


}
