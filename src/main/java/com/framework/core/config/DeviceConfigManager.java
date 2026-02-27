package com.framework.core.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.utils.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DeviceConfigManager - Thread-safe loader for devices.json.
 *
 * Loads device configurations from:
 *   src/main/resources/config/devices.json
 *
 * JSON structure:
 *   {
 *     "local":  { "android": [...], "ios": [...] },
 *     "remote": { "android": [...], "ios": [...] }
 *   }
 *
 * Usage:
 *   // Get a specific device by environment + platform + index
 *   DeviceConfig device = DeviceConfigManager.getDevice("local", "android", 0);
 *
 *   // Get all devices for an environment + platform
 *   List<DeviceConfig> devices = DeviceConfigManager.getDevices("local", "android");
 *
 *   // Round-robin: each parallel thread gets next available device
 *   DeviceConfig device = DeviceConfigManager.getNextDevice("local", "android");
 *
 * Thread-safe:
 *   - JSON loaded once (synchronized), cached in ConcurrentHashMap
 *   - Round-robin index uses AtomicInteger for thread-safe increment
 */
public final class DeviceConfigManager {

    private static final String CONFIG_PATH = "config/devices.json";

    // env -> platform -> List<DeviceConfig>  e.g. "local" -> "android" -> [device1, device2]
    private static final ConcurrentHashMap<String, Map<String, List<DeviceConfig>>> cache = new ConcurrentHashMap<>();

    // Round-robin counters per env+platform key  e.g. "local_android" -> AtomicInteger
    private static final ConcurrentHashMap<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    private static volatile boolean loaded = false;

    private DeviceConfigManager() {}

    // ── Public API ────────────────────────────────────────────

    /**
     * Get a specific device by environment, platform, and index.
     *
     * @param env      "local" or "remote"
     * @param platform "android" or "ios"
     * @param index    0-based index into the device array
     * @return DeviceConfig with platform and environment set ̰
     */
    public static DeviceConfig getDevice(String env, String platform, int index) {
        ensureLoaded();
        List<DeviceConfig> devices = getDevices(env, platform);

        if (index < 0 || index >= devices.size()) {
            throw new IllegalArgumentException(
                "Device index " + index + " out of range for " + env + "." + platform +
                " (available: " + devices.size() + " devices)"
            );
        }

        return devices.get(index);
    }

    /**
     * Get the next device in round-robin order.
     * Thread-safe: each parallel thread gets a different device.
     *
     * Example with 2 devices and 4 threads:
     *   Thread-1 → device[0], Thread-2 → device[1],
     *   Thread-3 → device[0], Thread-4 → device[1]
     *
     * @param env      "local" or "remote"
     * @param platform "android" or "ios"
     * @return next DeviceConfig in round-robin order
     */
    public static DeviceConfig getNextDevice(String env, String platform) {
        ensureLoaded();
        List<DeviceConfig> devices = getDevices(env, platform);

        if (devices.isEmpty()) {
            throw new IllegalArgumentException(
                "No devices configured for " + env + "." + platform + " in devices.json"
            );
        }

        String key = env.toLowerCase() + "_" + platform.toLowerCase();
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % devices.size();

        DeviceConfig device = devices.get(index);
        LoggerUtil.info("[DeviceConfig] Thread [" + Thread.currentThread().getName()
                + "] → " + device);
        return device;
    }

    /**
     * Get all devices for an environment and platform.
     *
     * @param env      "local" or "remote"
     * @param platform "android" or "ios"
     * @return unmodifiable list of DeviceConfig
     */
    public static List<DeviceConfig> getDevices(String env, String platform) {
        ensureLoaded();
        Map<String, List<DeviceConfig>> envMap = cache.get(env.toLowerCase());
        if (envMap == null) {
            throw new IllegalArgumentException(
                "Unknown environment '" + env + "' in devices.json. Available: " + cache.keySet()
            );
        }

        List<DeviceConfig> devices = envMap.get(platform.toLowerCase());
        return devices != null ? Collections.unmodifiableList(devices) : Collections.emptyList();
    }

    /**
     * Get device count for an environment + platform combo.
     */
    public static int getDeviceCount(String env, String platform) {
        return getDevices(env, platform).size();
    }

    /**
     * Force reload devices.json (e.g. if file was updated at runtime).
     */
    public static void reload() {
        loaded = false;
        cache.clear();
        roundRobinCounters.clear();
        ensureLoaded();
    }

    // ── Internal loading ──────────────────────────────────────

    private static synchronized void ensureLoaded() {
        if (loaded) return;

        try (InputStream is = DeviceConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_PATH)) {
            if (is == null) {
                throw new IllegalStateException("devices.json not found at: " + CONFIG_PATH);
            }

            ObjectMapper mapper = new ObjectMapper();

            // Parse: { "local": { "android": [...], "ios": [...] }, "remote": { ... } }
            Map<String, Map<String, List<DeviceConfig>>> raw = mapper.readValue(is,
                    new TypeReference<Map<String, Map<String, List<DeviceConfig>>>>() {});

            // Enrich each DeviceConfig with its environment and platform
            for (Map.Entry<String, Map<String, List<DeviceConfig>>> envEntry : raw.entrySet()) {
                String env = envEntry.getKey(); // "local" or "remote"

                for (Map.Entry<String, List<DeviceConfig>> platformEntry : envEntry.getValue().entrySet()) {
                    String platform = platformEntry.getKey(); // "android" or "ios"

                    for (DeviceConfig device : platformEntry.getValue()) {
                        device.setEnvironment(env);
                        device.setPlatform(platform);

                        // Resolve relative app paths to absolute
                        if (device.getAppPath() != null && !device.getAppPath().isEmpty()
                                && !device.getAppPath().startsWith("/")
                                && !device.getAppPath().startsWith("storage:") && !device.getAppPath().startsWith("sauce-storage")) {
                            device.setAppPath(System.getProperty("user.dir") + "/" + device.getAppPath());
                        }
                    }
                }

                cache.put(env.toLowerCase(), envEntry.getValue());
            }

            int totalDevices = cache.values().stream()
                    .flatMap(m -> m.values().stream())
                    .mapToInt(List::size)
                    .sum();

            LoggerUtil.info("[DeviceConfig] Loaded devices.json — " + totalDevices + " devices across "
                    + cache.keySet() + " environments");
            loaded = true;

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse devices.json: " + e.getMessage(), e);
        }
    }
}

