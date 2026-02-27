package com.framework.core.config;

import java.util.Map;

/**
 * DeviceConfig - POJO representing a single device configuration from devices.json.
 *
 * Used by DriverFactory to create a driver with the correct capabilities.
 * Each device in the JSON maps to one DeviceConfig instance.
 *
 * Example JSON entry:
 *   {
 *     "deviceName": "Pixel 9 Pro XL API 36.0",
 *     "platformVersion": "16",
 *     "appPackage": "com.thehomedepot.inhouse",
 *     "serverUrl": "http://127.0.0.1:4723"
 *   }
 */
public class DeviceConfig {

    // ── Common fields ─────────────────────────────────────────
    private String deviceName;
    private String platformVersion;
    private String appPath;
    private String serverUrl;
    private boolean noReset;

    // ── Android-specific ──────────────────────────────────────
    private String appPackage;
    private String appActivity;

    // ── iOS-specific ──────────────────────────────────────────
    private String bundleId;
    private boolean autoAcceptAlerts;

    // ── Cloud/Remote (Sauce Labs, BrowserStack, etc.) ─────────
    private Map<String, Object> cloudOptions;

    // ── Resolved at runtime (not from JSON) ───────────────────
    private String platform;   // "android" or "ios"
    private String environment; // "local" or "remote"

    // ── Getters & Setters ─────────────────────────────────────

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPlatformVersion() { return platformVersion; }
    public void setPlatformVersion(String platformVersion) { this.platformVersion = platformVersion; }

    public String getAppPath() { return appPath; }
    public void setAppPath(String appPath) { this.appPath = appPath; }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public boolean isNoReset() { return noReset; }
    public void setNoReset(boolean noReset) { this.noReset = noReset; }

    public String getAppPackage() { return appPackage; }
    public void setAppPackage(String appPackage) { this.appPackage = appPackage; }

    public String getAppActivity() { return appActivity; }
    public void setAppActivity(String appActivity) { this.appActivity = appActivity; }

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public boolean isAutoAcceptAlerts() { return autoAcceptAlerts; }
    public void setAutoAcceptAlerts(boolean autoAcceptAlerts) { this.autoAcceptAlerts = autoAcceptAlerts; }

    public Map<String, Object> getCloudOptions() { return cloudOptions; }
    public void setCloudOptions(Map<String, Object> cloudOptions) { this.cloudOptions = cloudOptions; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    @Override
    public String toString() {
        return platform + " | " + deviceName + " (" + platformVersion + ") [" + environment + "]";
    }
}

