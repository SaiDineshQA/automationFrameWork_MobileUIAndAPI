package com.framework.core.interfaces;

import org.testng.ITestResult;

/**
 * IReporter - Contract for all reporter implementations.
 * Dependency Inversion Principle: high-level modules depend on this abstraction.
 */
public interface IReporter {
    void initReport();
    void startTest(String testName, String description);
    void logStep(String message);
    void logPass(String message);
    void logFail(Throwable t, String screenshotPath);
    void logSkip(String message);
    void logInfo(String message);
    void attachScreenshot(String path);
    void flush();
}
