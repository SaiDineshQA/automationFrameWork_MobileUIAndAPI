package com.framework.tests.ui;

import com.framework.core.ai.visual.VisualAIEngine;
import com.framework.core.config.ConfigManager;
import com.framework.core.config.DeviceConfig;
import com.framework.core.config.DeviceConfigManager;
import com.framework.core.config.FrameworkModule;
import com.framework.core.driver.AppiumServerManager;
import com.framework.core.driver.DriverFactory;
import com.framework.core.driver.DriverManager;
import com.framework.core.interfaces.IDriver;
import com.framework.core.interfaces.IReporter;
import com.framework.core.reports.analytics.SmartAnalyticsReporter;
import com.framework.core.reports.extent.ExtentReporter;
import com.framework.pages.ui.HomePage;
import com.framework.pages.ui.LoginPage;
import com.framework.utils.LoggerUtil;
import com.framework.utils.ScreenshotUtil;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.appium.java_client.AppiumDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.lang.reflect.Method;

/**
 * BaseTest - Parent class for all test classes.
 *
 * Page objects are injected via Guice @Inject.
 * Injection happens AFTER driver creation in @BeforeMethod:
 *   1. Create driver → DriverManager.setDriver()
 *   2. Create Guice injector → injector.injectMembers(this)
 *   3. Guice creates LoginPage/HomePage → AbstractBase constructor
 *   4. Constructor pulls driver from DriverManager (already set in step 1)
 *
 * Thread safety:
 *   - Each thread has its own driver (ThreadLocal in DriverManager)
 *   - Each thread has its own ExtentTest node (static ThreadLocal in ExtentReporter)
 *   - Page objects are per-instance, per-method (Guice creates new ones each @BeforeMethod)
 *   - Shared counters in SmartAnalyticsReporter use AtomicInteger + CopyOnWriteArrayList
 *
 * To add more page objects in subclasses, just declare:
 *   @Inject protected MyNewPage myNewPage;
 * No other wiring needed — Guice + AbstractBase handle everything.
 */
public abstract class BaseTest {

    private static final IReporter reporter = new ExtentReporter();

    protected AppiumDriver driver;
    protected String platform;
    protected String environment;
    protected DeviceConfig deviceConfig;

    // ── Page objects injected by Guice (after driver is created) ──
    @Inject protected LoginPage loginPage;
    @Inject protected HomePage homePage;

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        reporter.initReport();

        // Start a single shared Appium server for all threads (local execution only)
        String env = ConfigManager.get("environment", "local");
        if (AppiumServerManager.isEnabled(env)) {
            AppiumServerManager.start();
        }

        LoggerUtil.info("═══════ SUITE STARTED ═══════");
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters({"environment", "platform", "deviceIndex"})
    public void methodSetup(
            @Optional("") String env,
            @Optional("") String plat,
            @Optional("-1") String deviceIdx,
            Method method) {

        // 1. Resolve environment and platform
        this.environment = env.isEmpty() ? ConfigManager.get("environment", "local") : env;
        this.platform    = plat.isEmpty() ? ConfigManager.get("platform", "android") : plat;

        // 2. Pick device from devices.json
        int index = Integer.parseInt(deviceIdx);
        this.deviceConfig = (index >= 0)
                ? DeviceConfigManager.getDevice(environment, platform, index)
                : DeviceConfigManager.getNextDevice(environment, platform);

        String testName = method.getName();
        String desc     = resolveDescription(method);

        LoggerUtil.info("▶ [" + Thread.currentThread().getName() + "] " + testName
                + " → " + deviceConfig);

        // 3. Override serverUrl with auto-started Appium server (if enabled)
        if (AppiumServerManager.isRunning()) {
            deviceConfig.setServerUrl(AppiumServerManager.getUrl().toString());
        }

        // 4. Create driver and store in ThreadLocal
        IDriver iDriver = DriverFactory.createDriver(deviceConfig);
        DriverManager.setDriver(iDriver);
        this.driver = (AppiumDriver) iDriver.getUnderlyingDriver();

        // 5. Guice injection — creates page objects AFTER driver is ready
        //    AbstractBase constructor → DriverManager.getDriver() ✅
        Injector injector = Guice.createInjector(new FrameworkModule());
        injector.injectMembers(this);
        reporter.startTest(testName, desc);
    }

    @AfterMethod(alwaysRun = true)
    public void methodTeardown(ITestResult result) {
        String name = result.getMethod().getMethodName();

        try {
            switch (result.getStatus()) {
                case ITestResult.FAILURE -> {
                    String screenshot = ScreenshotUtil.capture(name + "_FAILURE");
                    reporter.logFail(result.getThrowable(), screenshot);
                    LoggerUtil.error("❌ FAILED: " + name);
                }
                case ITestResult.SUCCESS -> {
                    reporter.logPass("Test completed successfully");
                    LoggerUtil.info("✅ PASSED: " + name);
                }
                case ITestResult.SKIP -> {
                    String reason = result.getThrowable() != null
                            ? result.getThrowable().getMessage() : "no reason";
                    reporter.logSkip(reason);
                    LoggerUtil.warn("⏭ SKIPPED: " + name);
                }
            }

            SmartAnalyticsReporter.record(result);
        } catch (Exception e) {
            LoggerUtil.error("[BaseTest] Error during teardown reporting: " + e.getMessage());
        } finally {
            // Always flush reporter and quit driver — even if reporting above failed
            try { reporter.flush(); } catch (Exception ignored) {}
            DriverManager.quitDriver();

            // Clear stale references for this test method
            this.driver       = null;
            this.deviceConfig = null;
        }
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        reporter.flush();
        SmartAnalyticsReporter.generateReport();
        AppiumServerManager.stop();
        LoggerUtil.info("═══════ SUITE COMPLETED ═══════");
    }


    // ── Convenience methods for subclass tests ────────────────

    protected void step(String description) {
        LoggerUtil.info("  STEP: " + description);
        reporter.logStep(description);
    }

    protected void attachScreenshot(String name) {
        String path = ScreenshotUtil.capture(name);
        reporter.attachScreenshot(path);
    }

    protected VisualAIEngine.VisualResult visualCheck(String checkpointName) {
        VisualAIEngine.VisualResult result = VisualAIEngine.compareWithBaseline(checkpointName);
        reporter.logInfo("[Visual] " + result.summary());
        if (!result.passed()) {
            reporter.attachScreenshot(result.diffImagePath());
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────

    private String resolveDescription(Method method) {
        Test annotation = method.getAnnotation(Test.class);
        return (annotation != null && !annotation.description().isEmpty())
                ? annotation.description()
                : method.getName();
    }
}
