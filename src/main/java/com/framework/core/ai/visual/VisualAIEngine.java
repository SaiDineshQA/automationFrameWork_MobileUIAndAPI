package com.framework.core.ai.visual;

import com.framework.core.driver.DriverManager;
import com.framework.core.exceptions.VisualComparisonException;
import com.framework.utils.LoggerUtil;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * VisualAIEngine - AI-powered visual regression engine.
 *
 * Single Responsibility: screenshot capture and image comparison only.
 * Stateless (no static mutable state) — safe for parallel execution.
 * Each thread captures its own screenshots independently.
 */
public class VisualAIEngine {

    private static final String BASELINE_DIR     = "src/test/resources/visual/baseline/";
    private static final String ACTUAL_DIR       = "test-output/visual/actual/";
    private static final String DIFF_DIR         = "test-output/visual/diff/";
    private static final double DEFAULT_TOLERANCE = 0.02;
    private static final int    COLOR_THRESHOLD  = 15;

    static {
        createDirectories(BASELINE_DIR, ACTUAL_DIR, DIFF_DIR);
    }

    private VisualAIEngine() {}

    /** Compare current screen with baseline using default tolerance (2%). */
    public static VisualResult compareWithBaseline(String checkpointName) {
        return compareWithBaseline(checkpointName, DEFAULT_TOLERANCE);
    }

    /** Compare current screen with baseline using a custom tolerance. */
    public static VisualResult compareWithBaseline(String checkpointName, double tolerance) {
        try {
            String threadSafeName = checkpointName + "_" + safeThreadName();
            BufferedImage actual  = captureAndSave(checkpointName);
            File baselineFile     = new File(BASELINE_DIR + checkpointName + ".png");

            if (!baselineFile.exists()) {
                ImageIO.write(actual, "PNG", baselineFile);
                LoggerUtil.info("[Visual] Baseline created: " + checkpointName);
                return VisualResult.baselineCreated(checkpointName);
            }

            BufferedImage baseline = ImageIO.read(baselineFile);
            return compare(checkpointName, baseline, actual, tolerance);

        } catch (IOException e) {
            throw new VisualComparisonException(checkpointName, e);
        }
    }

    /** Explicitly update the baseline for a given checkpoint. */
    public static void updateBaseline(String checkpointName) {
        try {
            BufferedImage screenshot = captureAndSave(checkpointName);
            ImageIO.write(screenshot, "PNG",
                    new File(BASELINE_DIR + checkpointName + ".png"));
            LoggerUtil.info("[Visual] Baseline updated: " + checkpointName);
        } catch (IOException e) {
            throw new VisualComparisonException(checkpointName, e);
        }
    }

    // ── Private helpers ───────────────────────────────────────

    private static VisualResult compare(String name, BufferedImage baseline,
                                         BufferedImage actual, double tolerance)
            throws IOException {
        if (baseline.getWidth() != actual.getWidth() ||
            baseline.getHeight() != actual.getHeight()) {
            actual = resize(actual, baseline.getWidth(), baseline.getHeight());
        }

        int width   = baseline.getWidth();
        int height  = baseline.getHeight();
        int total   = width * height;
        int diffPx  = 0;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color bColor = new Color(baseline.getRGB(x, y), true);
                Color aColor = new Color(actual.getRGB(x, y), true);
                boolean isDiff = !colorsSimilar(bColor, aColor);

                diffImage.setRGB(x, y, isDiff ? Color.RED.getRGB() : dimColor(aColor));
                if (isDiff) diffPx++;
            }
        }

        String diffPath = DIFF_DIR + name + "_diff_" + timestamp() + ".png";
        ImageIO.write(diffImage, "PNG", new File(diffPath));

        double similarity = 1.0 - ((double) diffPx / total);
        boolean passed    = (1.0 - similarity) <= tolerance;

        return new VisualResult(name, passed, similarity, diffPx, total, tolerance, diffPath);
    }

    private static BufferedImage captureAndSave(String name) throws IOException {
        File tmp = ((TakesScreenshot) DriverManager.getDriver().getUnderlyingDriver())
                .getScreenshotAs(OutputType.FILE);
        BufferedImage img = ImageIO.read(tmp);
        String path = ACTUAL_DIR + name + "_" + timestamp() + ".png";
        ImageIO.write(img, "PNG", new File(path));
        return img;
    }

    private static boolean colorsSimilar(Color c1, Color c2) {
        return Math.abs(c1.getRed()   - c2.getRed())   <= COLOR_THRESHOLD
            && Math.abs(c1.getGreen() - c2.getGreen()) <= COLOR_THRESHOLD
            && Math.abs(c1.getBlue()  - c2.getBlue())  <= COLOR_THRESHOLD;
    }

    private static int dimColor(Color c) {
        return new Color((int)(c.getRed()*0.6), (int)(c.getGreen()*0.6),
                (int)(c.getBlue()*0.6), c.getAlpha()).getRGB();
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, src.getType());
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static String safeThreadName() {
        return Thread.currentThread().getName().replaceAll("[^a-zA-Z0-9]", "_");
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
    }

    private static void createDirectories(String... dirs) {
        for (String dir : dirs) {
            try { Files.createDirectories(Paths.get(dir)); }
            catch (IOException ignored) {}
        }
    }

    // ── Result value object ───────────────────────────────────

    public record VisualResult(
        String checkpointName,
        boolean passed,
        double similarity,
        int differentPixels,
        int totalPixels,
        double tolerance,
        String diffImagePath
    ) {
        public String similarityPct() {
            return String.format("%.2f%%", similarity * 100);
        }

        public String summary() {
            return String.format("%s | Similar: %s | Diff px: %d/%d | Tolerance: %.1f%%",
                passed ? "PASS" : "FAIL", similarityPct(),
                differentPixels, totalPixels, tolerance * 100);
        }

        static VisualResult baselineCreated(String name) {
            return new VisualResult(name, true, 1.0, 0, 0, 0, "");
        }
    }
}
