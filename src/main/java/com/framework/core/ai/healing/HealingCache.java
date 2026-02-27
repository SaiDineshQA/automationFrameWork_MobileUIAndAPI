package com.framework.core.ai.healing;

import com.framework.utils.LoggerUtil;
import org.openqa.selenium.By;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HealingCache - Thread-safe store for healed locators and healing events.
 *
 * Single Responsibility: caching and event history only.
 * ConcurrentHashMap ensures safe reads/writes across parallel threads.
 * CopyOnWriteArrayList ensures event log is safe for concurrent appends.
 */
public class HealingCache {

    private static final Map<String, By> healedLocators  = new ConcurrentHashMap<>();
    private static final List<HealingEvent> healingEvents = new CopyOnWriteArrayList<>();

    private HealingCache() {}

    public static Optional<By> getCachedLocator(String originalLocatorKey) {
        return Optional.ofNullable(healedLocators.get(originalLocatorKey));
    }

    public static void cacheHealedLocator(String originalKey, By healedLocator,
                                           String elementName) {
        healedLocators.put(originalKey, healedLocator);
        HealingEvent event = new HealingEvent(
            elementName, originalKey, healedLocator.toString(),
            Thread.currentThread().getName(), System.currentTimeMillis()
        );
        healingEvents.add(event);
        LoggerUtil.info(String.format(
            "[Healing] Cached healed locator for '%s' on thread [%s]: %s",
            elementName, event.threadName, healedLocator
        ));
    }

    public static void invalidate(String originalKey) {
        healedLocators.remove(originalKey);
    }

    /** Returns unmodifiable snapshot — safe for reporting across threads */
    public static List<HealingEvent> getEvents() {
        return Collections.unmodifiableList(healingEvents);
    }

    public static void clearEvents() {
        healingEvents.clear();
    }

    public static int getHealedCount() {
        return healedLocators.size();
    }

    // ── Value object ──────────────────────────────────────────

    public record HealingEvent(
        String elementName,
        String originalLocator,
        String healedLocator,
        String threadName,
        long timestamp
    ) {}
}
