package com.framework.core.listeners;

import com.framework.utils.LoggerUtil;
import org.testng.*;

public class TestListener implements ITestListener, ISuiteListener {
    @Override public void onStart(ISuite suite)    { LoggerUtil.info("Suite started:  " + suite.getName()); }
    @Override public void onFinish(ISuite suite)   { LoggerUtil.info("Suite finished: " + suite.getName()); }
    @Override public void onStart(ITestContext ctx) { LoggerUtil.info("Context: " + ctx.getName()); }
    @Override public void onFinish(ITestContext ctx) {
        LoggerUtil.info(String.format("Context done: %s | Pass:%d Fail:%d Skip:%d",
            ctx.getName(), ctx.getPassedTests().size(),
            ctx.getFailedTests().size(), ctx.getSkippedTests().size()));
    }
    @Override public void onTestStart(ITestResult r)   { LoggerUtil.info("▶ " + r.getMethod().getMethodName() + " [" + Thread.currentThread().getName() + "]"); }
    @Override public void onTestSuccess(ITestResult r) { LoggerUtil.info("✅ " + r.getMethod().getMethodName()); }
    @Override public void onTestFailure(ITestResult r) { LoggerUtil.error("❌ " + r.getMethod().getMethodName() + " — " + r.getThrowable().getMessage()); }
    @Override public void onTestSkipped(ITestResult r) { LoggerUtil.warn("⏭ " + r.getMethod().getMethodName()); }
}
