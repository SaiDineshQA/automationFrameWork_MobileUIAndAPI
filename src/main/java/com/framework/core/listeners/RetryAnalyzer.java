package com.framework.core.listeners;

import com.framework.core.config.ConfigManager;
import com.framework.utils.LoggerUtil;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer - Automatically retries failed tests.
 *
 * Reads max retry count from config.properties:
 *   retry.max.count=2    (default: 0 = no retries)
 *
 * Override at runtime:
 *   mvn test -Dretry.max.count=3
 *
 * Thread-safe: each test instance gets its own RetryAnalyzer
 * (TestNG creates a new instance per test method).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int currentRetry = 0;

    @Override
    public boolean retry(ITestResult result) {
        int maxRetry = ConfigManager.getInt("retry.max.count", 0);

        if (currentRetry < maxRetry) {
            currentRetry++;
            LoggerUtil.warn(String.format(
                    "🔄 RETRY [%d/%d]: %s [thread: %s]",
                    currentRetry, maxRetry,
                    result.getMethod().getMethodName(),
                    Thread.currentThread().getName()
            ));
            return true;
        }
        return false;
    }
}

