package com.framework.utils;

import com.framework.core.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
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

    public static byte[] takeScreenShot(TakesScreenshot driver) {
        return driver.getScreenshotAs(OutputType.BYTES);
    }

    public static String toBase64(String path) {
        try { return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(path))); }
        catch (Exception e) { return ""; }
    }

    private static boolean match(int rgb1, int rgb2, int tol) {
        return Math.abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF)) <= tol &&
                Math.abs(((rgb1 >>  8) & 0xFF) - ((rgb2 >>  8) & 0xFF)) <= tol &&
                Math.abs(( rgb1        & 0xFF) - ( rgb2        & 0xFF)) <= tol;
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, src.getType());
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    public static String save(byte[] bytes, String dir, String name) throws IOException {
        Files.createDirectories(Paths.get(dir));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String path = dir + "/" + name + "_" + timestamp + ".png";
        Files.write(Paths.get(path), bytes);
        LoggerUtil.debug("Screenshot: {}"+ path);
        return path;
    }


    /** Crop to a bounding box */
    public static byte[] crop(byte[] fullShot, org.openqa.selenium.Rectangle bounds) throws IOException {
        BufferedImage full = ImageIO.read(new ByteArrayInputStream(fullShot));
        int x = Math.max(0, bounds.x);
        int y = Math.max(0, bounds.y);
        int w = Math.min(bounds.width,  full.getWidth()  - x);
        int h = Math.min(bounds.height, full.getHeight() - y);
        BufferedImage cropped = full.getSubimage(x, y, w, h);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(cropped, "PNG", baos);
        return baos.toByteArray();
    }
}
