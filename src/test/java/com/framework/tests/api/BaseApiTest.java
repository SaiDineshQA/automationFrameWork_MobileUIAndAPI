package com.framework.tests.api;

import com.framework.core.interfaces.IReporter;
import com.framework.core.reports.analytics.SmartAnalyticsReporter;
import com.framework.core.reports.extent.ExtentReporter;
import com.framework.pages.api.CreateAccountApi;
import com.framework.pages.api.GetAccountDetailsApi;
import com.framework.pages.api.GetUserInfoApi;
import com.framework.utils.LoggerUtil;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.lang.reflect.Method;

/**
 * BaseApiTest - Parent class for all API test classes.
 *
 * API page objects are pre-created here. Tests call setResponse() and validate:
 *
 *   getAccountDetailsApi.setResponse(response);
 *   int id = getAccountDetailsApi.getInt(GetAccountDetailsApi.Fields.ID);
 *   Assert.assertEquals(id, 1, "ID mismatch");
 */
public abstract class BaseApiTest {

    private static final IReporter reporter = new ExtentReporter();

    // ── API page objects (pre-created, reusable) ──
    protected GetAccountDetailsApi getAccountDetailsApi = new GetAccountDetailsApi();
    protected GetUserInfoApi getUserInfoApi = new GetUserInfoApi();
    protected CreateAccountApi createAccountApi = new CreateAccountApi();

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        reporter.initReport();
        LoggerUtil.info("═══════ API SUITE STARTED ═══════");
    }

    @BeforeMethod(alwaysRun = true)
    public void methodSetup(Method method) {
        String testName = method.getName();
        String desc = resolveDescription(method);
        reporter.startTest(testName, desc);
        LoggerUtil.info("▶ [" + Thread.currentThread().getName() + "] " + testName);
    }

    @AfterMethod(alwaysRun = true)
    public void methodTeardown(ITestResult result) {
        String name = result.getMethod().getMethodName();

        try {
            switch (result.getStatus()) {
                case ITestResult.FAILURE -> {
                    reporter.logFail(result.getThrowable(), null);
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
            LoggerUtil.error("[BaseApiTest] Error during teardown reporting: " + e.getMessage());
        } finally {
            try { reporter.flush(); } catch (Exception ignored) {}
        }
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        reporter.flush();
        SmartAnalyticsReporter.generateReport();
        LoggerUtil.info("═══════ API SUITE COMPLETED ═══════");
    }

    protected void step(String description) {
        LoggerUtil.info("  STEP: " + description);
        reporter.logStep(description);
    }

    private String resolveDescription(Method method) {
        Test annotation = method.getAnnotation(Test.class);
        return (annotation != null && !annotation.description().isEmpty())
                ? annotation.description()
                : method.getName();
    }
}
