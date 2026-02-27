package com.framework.core.driver;

import com.framework.core.interfaces.IDriver;
import com.framework.utils.LoggerUtil;

/**
 * DriverManager - Thread-safe IDriver registry using ThreadLocal.
 *
 * Single Responsibility: manages driver lifecycle per thread only.
 * Guarantees each parallel test thread gets its own isolated driver.
 * No static mutable state shared across threads.
 */
public final class DriverManager {

    private static final ThreadLocal<IDriver> driverHolder = new ThreadLocal<>();

    private DriverManager() {}

    public static void setDriver(IDriver driver) {
        driverHolder.set(driver);
    }

    public static IDriver getDriver() {
        IDriver driver = driverHolder.get();
        if (driver == null) {
            throw new IllegalStateException(
                "No driver found for thread [" + Thread.currentThread().getName() + "]. " +
                "Ensure initDriver() was called in @BeforeMethod."
            );
        }
        return driver;
    }

    public static boolean isInitialized() {
        return driverHolder.get() != null;
    }

    public static void quitDriver() {
        IDriver driver = driverHolder.get();
        if (driver != null) {
            try {
                driver.quit();
                LoggerUtil.info("[Driver] Quit for thread: " + Thread.currentThread().getName());
            } catch (Exception e) {
                LoggerUtil.warn("[Driver] Error during quit: " + e.getMessage());
            } finally {
                driverHolder.remove(); // Critical: prevents memory leaks in thread pools
            }
        }
    }
}
