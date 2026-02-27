package com.framework.core.mobile.context;

import com.framework.utils.LoggerUtil;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.remote.SupportsContextSwitching;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.Set;

/**
 * ContextSwitcher - Handles mobile app context switching.
 *
 * Mobile apps can have multiple contexts:
 * - NATIVE_APP: Native UI elements (Android/iOS SDK widgets)
 * - WEBVIEW_<id>: Web content (HTML/CSS rendered in WebView)
 *
 * Use cases:
 * - Hybrid apps (native + web content)
 * - In-app browsers
 * - OAuth/payment flows opening web pages
 *
 * Thread-safe: operates on driver passed as parameter.
 */
public class ContextSwitcher {

    private ContextSwitcher() {}

    /**
     * Switch to native app context.
     * Use when automating native UI elements.
     */
    public static void switchToNative(AppiumDriver driver) {
        try {
            ((SupportsContextSwitching) driver).context("NATIVE_APP");
            LoggerUtil.info("[Context] Switched to NATIVE_APP");
        } catch (Exception e) {
            LoggerUtil.error("[Context] Failed to switch to NATIVE_APP: " + e.getMessage());
            throw new ContextSwitchException("NATIVE_APP", e);
        }
    }

    /**
     * Switch to first available WebView context.
     * Use when automating web content inside the app.
     */
    public static void switchToWebView(AppiumDriver driver) {
        Set<String> contexts = ((SupportsContextSwitching) driver).getContextHandles();
        LoggerUtil.info("[Context] Available contexts: " + contexts);

        for (String context : contexts) {
            if (context.contains("WEBVIEW")) {
                ((SupportsContextSwitching) driver).context(context);
                LoggerUtil.info("[Context] Switched to: " + context);
                return;
            }
        }

        throw new ContextSwitchException("No WEBVIEW context found. Available: " + contexts);
    }

    /**
     * Switch to specific WebView by index (when multiple WebViews exist).
     * @param index 0-based index (e.g., 0 for first WebView, 1 for second)
     */
    public static void switchToWebView(AppiumDriver driver, int index) {
        Set<String> contexts = ((SupportsContextSwitching) driver).getContextHandles();
        String[] webviews = contexts.stream()
                .filter(c -> c.contains("WEBVIEW"))
                .toArray(String[]::new);

        if (index >= webviews.length) {
            throw new ContextSwitchException(
                    "WebView index " + index + " not found. Only " + webviews.length + " WebViews available."
            );
        }

        ((SupportsContextSwitching) driver).context(webviews[index]);
        LoggerUtil.info("[Context] Switched to WebView[" + index + "]: " + webviews[index]);
    }

    /**
     * Switch to WebView by name/pattern.
     * @param namePattern partial match (e.g., "chrome" matches "WEBVIEW_chrome_12345")
     */
    public static void switchToWebView(AppiumDriver driver, String namePattern) {
        Set<String> contexts = ((SupportsContextSwitching) driver).getContextHandles();

        for (String context : contexts) {
            if (context.contains("WEBVIEW") && context.toLowerCase().contains(namePattern.toLowerCase())) {
                ((SupportsContextSwitching) driver).context(context);
                LoggerUtil.info("[Context] Switched to: " + context);
                return;
            }
        }

        throw new ContextSwitchException(
                "No WEBVIEW matching '" + namePattern + "' found. Available: " + contexts
        );
    }

    /**
     * Wait for WebView to appear and switch to it.
     * Useful when WebView loads dynamically.
     */
    public static void waitAndSwitchToWebView(AppiumDriver driver, Duration timeout) {
        FluentWait<AppiumDriver> wait = new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);

        wait.until(d -> {
            Set<String> contexts = ((SupportsContextSwitching) d).getContextHandles();
            LoggerUtil.debug("[Context] Waiting for WebView... Available: " + contexts);
            return contexts.stream().anyMatch(c -> c.contains("WEBVIEW"));
        });

        switchToWebView(driver);
    }

    /**
     * Get current context.
     */
    public static String getCurrentContext(AppiumDriver driver) {
        String context = ((SupportsContextSwitching) driver).getContext();
        LoggerUtil.debug("[Context] Current context: " + context);
        return context;
    }

    /**
     * Get all available contexts.
     */
    public static Set<String> getAllContexts(AppiumDriver driver) {
        Set<String> contexts = ((SupportsContextSwitching) driver).getContextHandles();
        LoggerUtil.info("[Context] All contexts: " + contexts);
        return contexts;
    }

    /**
     * Check if currently in native context.
     */
    public static boolean isNativeContext(AppiumDriver driver) {
        return getCurrentContext(driver).equals("NATIVE_APP");
    }

    /**
     * Check if currently in WebView context.
     */
    public static boolean isWebViewContext(AppiumDriver driver) {
        return getCurrentContext(driver).contains("WEBVIEW");
    }

    /**
     * Execute action in WebView, then switch back to native.
     * Automatically handles context switching.
     */
    public static <T> T executeInWebView(AppiumDriver driver, java.util.function.Function<AppiumDriver, T> action) {
        String originalContext = getCurrentContext(driver);
        try {
            switchToWebView(driver);
            return action.apply(driver);
        } finally {
            ((SupportsContextSwitching) driver).context(originalContext);
            LoggerUtil.info("[Context] Restored context: " + originalContext);
        }
    }

    /**
     * Execute action in native context, then switch back to original.
     */
    public static <T> T executeInNative(AppiumDriver driver, java.util.function.Function<AppiumDriver, T> action) {
        String originalContext = getCurrentContext(driver);
        try {
            switchToNative(driver);
            return action.apply(driver);
        } finally {
            ((SupportsContextSwitching) driver).context(originalContext);
            LoggerUtil.info("[Context] Restored context: " + originalContext);
        }
    }

    // ── Exception ─────────────────────────────────────────────

    public static class ContextSwitchException extends RuntimeException {
        public ContextSwitchException(String message) {
            super("[Context Switch Failed] " + message);
        }
        public ContextSwitchException(String context, Throwable cause) {
            super("[Context Switch Failed] Could not switch to: " + context, cause);
        }
    }
}
