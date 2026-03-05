package com.framework.core.ai.healing;

import com.framework.core.exceptions.HealingFailedException;
import com.framework.core.interfaces.IDriver;
import com.framework.core.interfaces.ISelfHealer;
import com.framework.utils.LoggerUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SelfHealingDriver - Implements ISelfHealer with YAML OR condition support.
 *
 * Single Responsibility: orchestrates healing flow only.
 * Depends on abstractions: ISelfHealer, IDriver.
 * Delegates to: YamlLocatorRepository (OR conditions), AlternativeLocatorStrategy (generation), HealingCache (storage).
 *
 * Enhanced healing flow with YAML OR conditions:
 * 1. Try original locator
 * 2. Check HealingCache for previously healed locator
 * 3. Try OR conditions from YAML (if defined: xpath::value | css::value)
 * 4. Generate alternatives via AlternativeLocatorStrategy
 * 5. Try each alternative; cache the first that works
 * 6. If all fail → throw HealingFailedException
 *
 * Thread-safe: ThreadLocal logs, all delegates are thread-safe.
 */
public class SelfHealingDriver implements ISelfHealer {

    // Thread-local log so parallel tests don't mix healing logs
    private final ThreadLocal<List<String>> threadLog =
            ThreadLocal.withInitial(ArrayList::new);

    @Override
    public WebElement heal(By failedLocator, String elementName, IDriver driver) {
        return heal(List.of(failedLocator), elementName, driver);
    }

    @Override
    public WebElement heal(List<By> failedLocators, String elementName, IDriver driver) {
        String locatorKey = failedLocators.toString();

        LoggerUtil.warn(String.format(
            "[Healing] All %d YAML strategies failed for '%s'. Starting self-healing... [thread: %s]",
            failedLocators.size(), elementName, Thread.currentThread().getName()
        ));

        // Step 1: Check cache for a previously healed locator
        Optional<By> cached = HealingCache.getCachedLocator(locatorKey);
        if (cached.isPresent()) {
            try {
                WebElement element = driver.findElement(cached.get());
                threadLog.get().add("Used cached healed locator for: " + elementName);
                LoggerUtil.info("[Healing] Cache hit for '" + elementName + "': " + cached.get());
                return element;
            } catch (NoSuchElementException e) {
                HealingCache.invalidate(locatorKey);
                LoggerUtil.warn("[Healing] Cached locator became stale for: " + elementName);
            }
        }

        // Step 2: Generate alternatives from EVERY failed locator (not just primary)
        List<By> allAlternatives = new ArrayList<>();
        for (By failedLocator : failedLocators) {
            allAlternatives.addAll(AlternativeLocatorStrategy.generate(failedLocator, elementName));
        }
        LoggerUtil.info("[Healing] Trying " + allAlternatives.size()
                + " alternative strategies (from " + failedLocators.size() + " locators) for: " + elementName);

        for (By alternative : allAlternatives) {
            try {
                WebElement element = driver.findElement(alternative);
                HealingCache.cacheHealedLocator(locatorKey, alternative, elementName);
                String logEntry = String.format(
                    "Healed '%s': %s → [%s]", elementName, locatorKey, alternative
                );
                threadLog.get().add(logEntry);
                LoggerUtil.info("[Healing SUCCESS] " + logEntry);
                return element;
            } catch (NoSuchElementException ignored) {
                // Try next alternative
            }
        }

        // Step 3: All strategies exhausted
        throw new HealingFailedException(elementName, locatorKey);
    }


    @Override
    public List<String> getHealingLog() {
        return List.copyOf(threadLog.get()); // Immutable snapshot
    }

    @Override
    public void clearLog() {
        threadLog.get().clear();
    }

    /**
     * Removes the ThreadLocal entry entirely — prevents memory leaks in thread pools.
     * Call this in @AfterMethod after reading the healing log.
     */
    public void removeLog() {
        threadLog.remove();
    }
}
