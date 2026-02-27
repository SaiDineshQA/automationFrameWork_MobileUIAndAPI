package com.framework.core.driver;

import com.framework.core.config.ConfigManager;
import com.framework.core.config.DeviceConfig;
import com.framework.core.config.DeviceConfigManager;
import com.framework.core.exceptions.DriverInitException;
import com.framework.core.interfaces.IDriver;
import com.framework.utils.LoggerUtil;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * DriverFactory - Creates drivers from DeviceConfig (loaded from devices.json).
 *
 * Supports:
 * - Local Appium server (local.android / local.ios)
 * - Remote / Cloud (remote.android / remote.ios with cloudOptions)
 *
 * Usage:
 *   DeviceConfig device = DeviceConfigManager.getNextDevice("local", "android");
 *   IDriver driver = DriverFactory.createDriver(device);
 */
public class DriverFactory {

    private static final int DEFAULT_WAIT_SECS = 10;

    private DriverFactory() {}

    /**
     * Creates a driver from a DeviceConfig (from devices.json).
     * This is the primary entry point for all driver creation.
     */
    public static IDriver createDriver(DeviceConfig device) {
        try {
            String platform = device.getPlatform().toLowerCase();

            // Resolve cloud credentials — safe for parallel calls:
            // ${...} placeholders resolve to the same env var values each time,
            // and URL auth insertion is guarded by !contains("@")
            resolveCloudCredentials(device);
            URL serverUrl = new URL(device.getServerUrl());

            AppiumDriver driver = switch (platform) {
                case "ios"     -> createIOSDriver(serverUrl, device);
                case "android" -> createAndroidDriver(serverUrl, device);
                default -> throw new DriverInitException(platform,
                        new IllegalArgumentException("Unsupported platform: " + platform));
            };

            driver.manage().timeouts().implicitlyWait(
                    Duration.ofSeconds(ConfigManager.getInt("implicit.wait", DEFAULT_WAIT_SECS))
            );

            LoggerUtil.info("[DriverFactory] Created " + device
                    + " [thread: " + Thread.currentThread().getName() + "]");

            return new AppiumDriverWrapper(driver, platform);

        } catch (MalformedURLException e) {
            throw new DriverInitException(device.getPlatform(), e);
        }
    }

    // ── Android ───────────────────────────────────────────────

    private static AndroidDriver createAndroidDriver(URL serverUrl, DeviceConfig device) {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName(device.getPlatform());
        options.setAutomationName("UiAutomator2");
        options.setDeviceName(device.getDeviceName());
        options.setPlatformVersion(device.getPlatformVersion());
        options.setNewCommandTimeout(Duration.ofSeconds(300));
        options.setAutoGrantPermissions(true);
        options.setNoReset(device.isNoReset());

        if (device.getAppPackage() != null && !device.getAppPackage().isEmpty()) {
            options.setAppPackage(device.getAppPackage());
        }
        if (device.getAppActivity() != null && !device.getAppActivity().isEmpty()) {
            options.setAppActivity(device.getAppActivity());
        }
        if (device.getAppPath() != null && !device.getAppPath().isEmpty()) {
            options.setApp(device.getAppPath());
        }

        // Cloud options (Sauce Labs, BrowserStack, etc.)
        applyCloudOptions(options, device);

        return new AndroidDriver(serverUrl, options);
    }

    // ── iOS ───────────────────────────────────────────────────

    private static IOSDriver createIOSDriver(URL serverUrl, DeviceConfig device) {
        XCUITestOptions options = new XCUITestOptions();
        options.setPlatformName(device.getPlatform());
        options.setAutomationName("XCUITest");
        options.setDeviceName(device.getDeviceName());
        options.setPlatformVersion(device.getPlatformVersion());
        options.setNewCommandTimeout(Duration.ofSeconds(300));
        options.setAutoAcceptAlerts(device.isAutoAcceptAlerts());

        if (device.getBundleId() != null && !device.getBundleId().isEmpty()) {
            options.setBundleId(device.getBundleId());
        }
        if (device.getAppPath() != null && !device.getAppPath().isEmpty()) {
            options.setApp(device.getAppPath());
        }

        // Cloud options (Sauce Labs, BrowserStack, etc.)
        applyCloudOptions(options, device);

        return new IOSDriver(serverUrl, options);
    }

    // ── Cloud options helper ──────────────────────────────────

    private static void applyCloudOptions(Object options, DeviceConfig device) {
        Map<String, Object> cloud = device.getCloudOptions();
        if (cloud == null || cloud.isEmpty()) return;

        for (Map.Entry<String, Object> entry : cloud.entrySet()) {
            if (options instanceof UiAutomator2Options uiOpts) {
                uiOpts.setCapability(entry.getKey(), entry.getValue());
            } else if (options instanceof XCUITestOptions xcOpts) {
                xcOpts.setCapability(entry.getKey(), entry.getValue());
            }
        }
    }

    // ── Cloud credential resolution ───────────────────────────

    /**
     * Resolves ${ENV_VAR} placeholders in cloudOptions and builds
     * authenticated Sauce Labs URL if credentials are present.
     *
     * Reads from: System property → Environment variable → placeholder stays as-is
     *
     * Example:
     *   devices.json:  "username": "${SAUCE_USERNAME}"
     *   Env var:       SAUCE_USERNAME=myuser
     *   Result:        "username": "myuser"
     *   URL becomes:   https://myuser:accesskey@ondemand.us-west-1.saucelabs.com:443/wd/hub
     */
    @SuppressWarnings("unchecked")
    private static void resolveCloudCredentials(DeviceConfig device) {
        Map<String, Object> cloud = device.getCloudOptions();
        if (cloud == null || cloud.isEmpty()) return;

        String username = null;
        String accessKey = null;

        // Resolve ${...} placeholders in all cloudOptions entries
        for (Map.Entry<String, Object> entry : cloud.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Nested map (e.g., "sauce:options": { "username": "${SAUCE_USERNAME}", ... })
                Map<String, Object> nested = (Map<String, Object>) value;
                for (Map.Entry<String, Object> nestedEntry : nested.entrySet()) {
                    if (nestedEntry.getValue() instanceof String strVal) {
                        String resolved = resolveEnvPlaceholder(strVal);
                        nestedEntry.setValue(resolved);

                        // Capture username/accessKey for URL auth
                        if ("username".equalsIgnoreCase(nestedEntry.getKey())) {
                            username = resolved;
                        } else if ("accessKey".equalsIgnoreCase(nestedEntry.getKey())) {
                            accessKey = resolved;
                        }
                    }
                }
            } else if (value instanceof String strVal) {
                entry.setValue(resolveEnvPlaceholder(strVal));
            }
        }

        // Build authenticated URL: https://user:key@host/wd/hub
        if (username != null && accessKey != null) {
            String serverUrl = device.getServerUrl();
            if (!serverUrl.contains("@")) {
                // Insert credentials into URL: https://HOST → https://user:key@HOST
                serverUrl = serverUrl.replaceFirst("(https?://)", "$1" + username + ":" + accessKey + "@");
                device.setServerUrl(serverUrl);
                LoggerUtil.info("[DriverFactory] Sauce Labs URL built for user: " + username);
            }
        }
    }

    /**
     * Resolves a ${ENV_VAR} placeholder string.
     * Priority: System property → Environment variable → original string
     */
    private static String resolveEnvPlaceholder(String value) {
        if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }

        String envKey = value.substring(2, value.length() - 1); // "SAUCE_USERNAME"

        // Try system property first (e.g., -DSAUCE_USERNAME=xxx)
        String resolved = System.getProperty(envKey);
        if (resolved != null && !resolved.isEmpty()) return resolved;

        // Try environment variable
        resolved = System.getenv(envKey);
        if (resolved != null && !resolved.isEmpty()) return resolved;

        LoggerUtil.warn("[DriverFactory] Environment variable not set: " + envKey
                + " (used in devices.json cloudOptions)");
        return value;
    }
}
