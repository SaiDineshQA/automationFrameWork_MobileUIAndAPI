package com.framework.core.reports.extent;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.core.config.ConfigManager;
import com.framework.core.interfaces.IReporter;
import com.framework.utils.LoggerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ExtentReporter - Thread-safe HTML reporter implementing IReporter.
 *
 * Single Responsibility: Extent Reports integration only.
 * Thread safety:
 *   - ExtentReports instance is shared (Extent is thread-safe for createTest)
 *   - ExtentTest nodes are stored in ThreadLocal — one node per thread
 *   - flush() is called once at suite end (not per-test)
 *
 * DIP: callers depend on IReporter, not this class directly.
 */
public class ExtentReporter implements IReporter {

    private static volatile ExtentReports extentReports;

    // Static ThreadLocal: shared across all ExtentReporter instances
    // Each parallel thread stores its own ExtentTest node
    private static final ThreadLocal<ExtentTest> testNodeHolder = new ThreadLocal<>();

    @Override
    public void initReport() {
        String dir  = "test-output/extent-report/";
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            LoggerUtil.warn("[Report] Could not create report directory: " + e.getMessage());
        }

        String path = dir + "Mobile_Report_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(path);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("Mobile Automation Report");
        spark.config().setReportName("Mobile Framework");
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);
        extentReports.setSystemInfo("Platform",   ConfigManager.get("platform", "android"));
        extentReports.setSystemInfo("Grid",       ConfigManager.getBoolean("grid.enabled", false) ? "Enabled" : "Local");
        extentReports.setSystemInfo("AI Features","Self-Healing | Visual AI | Test Gen | Analytics");
        LoggerUtil.info("[Report] Extent report initialized: " + path);
    }

    @Override
    public void startTest(String testName, String description) {
        // ExtentReports.createTest() is thread-safe; each thread stores its own node
        ExtentTest node = extentReports.createTest(testName, description);
        node.info("Thread: " + Thread.currentThread().getName());
        testNodeHolder.set(node);
    }

    @Override
    public void logStep(String message) {
        getNode().info(message);
    }

    @Override
    public void logPass(String message) {
        getNode().pass(message);
    }

    @Override
    public void logFail(Throwable t, String screenshotPath) {
        getNode().fail(t);
        if (screenshotPath != null && !screenshotPath.isEmpty()) {
            try { getNode().addScreenCaptureFromPath(screenshotPath, "Failure Screenshot"); }
            catch (Exception e) { LoggerUtil.warn("[Report] Could not attach screenshot: " + e.getMessage()); }
        }
    }

    @Override
    public void logSkip(String message) {
        getNode().skip(message);
    }

    @Override
    public void logInfo(String message) {
        getNode().info(message);
    }

    @Override
    public void attachScreenshot(String path) {
        if (path == null || path.isEmpty()) return;
        try { getNode().addScreenCaptureFromPath(path); }
        catch (Exception e) { LoggerUtil.warn("[Report] Screenshot attach failed: " + e.getMessage()); }
    }

    @Override
    public void flush() {
        if (extentReports != null) extentReports.flush();
        testNodeHolder.remove(); // prevent ThreadLocal memory leak in thread pools
    }

    private ExtentTest getNode() {
        ExtentTest node = testNodeHolder.get();
        if (node == null) {
            throw new IllegalStateException(
                "No ExtentTest node for thread [" + Thread.currentThread().getName() +
                "]. Call startTest() first."
            );
        }
        return node;
    }
}
