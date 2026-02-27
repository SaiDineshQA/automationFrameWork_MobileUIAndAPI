package com.framework.utils;

import com.framework.core.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/** ScreenshotUtil - stateless screenshot helper. Thread-safe (no shared state). */
public final class ScreenshotUtil {

    private static final String DIR = "test-output/screenshots/";

    static {
        try { Files.createDirectories(Paths.get(DIR)); } catch (IOException ignored) {}
    }

    private ScreenshotUtil() {}

    public static String capture(String name) {
        try {
            if (!DriverManager.isInitialized()) return "";
            File tmp = ((TakesScreenshot) DriverManager.getDriver().getUnderlyingDriver())
                    .getScreenshotAs(OutputType.FILE);
            // Include thread name — prevents filename collision in parallel runs
            String safe   = Thread.currentThread().getName().replaceAll("[^a-zA-Z0-9]", "_");
            String ts     = new SimpleDateFormat("HHmmss_SSS").format(new Date());
            String path   = DIR + name + "_" + safe + "_" + ts + ".png";
            Files.copy(tmp.toPath(), Paths.get(path));
            return path;
        } catch (Exception e) {
            LoggerUtil.warn("[Screenshot] Failed: " + e.getMessage());
            return "";
        }
    }
}
