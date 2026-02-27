package com.framework.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager - Immutable config loader. Thread-safe (Properties loaded once at class-init).
 * Priority: System property > Environment variable > config file > default value.
 */
public final class ConfigManager {

    private static final Properties props = new Properties();

    static {
        loadFile("config/config.properties");
        String env = System.getProperty("env", "");
        if (!env.isEmpty()) loadFile("config/config-" + env + ".properties");
    }

    private ConfigManager() {}

    public static String get(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null) return sys;
        String envVar = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envVar != null) return envVar;
        return props.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(get(key)); } catch (NumberFormatException e) { return def; }
    }

    public static boolean getBoolean(String key, boolean def) {
        String v = get(key); return v.isEmpty() ? def : Boolean.parseBoolean(v);
    }

    private static void loadFile(String path) {
        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
    }
}
