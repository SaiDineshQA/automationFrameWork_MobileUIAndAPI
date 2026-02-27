package com.framework.core.reports.analytics;

import com.framework.core.ai.healing.HealingCache;
import com.framework.utils.LoggerUtil;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * SmartAnalyticsReporter - Thread-safe AI analytics dashboard generator.
 *
 * Single Responsibility: collects metrics, generates one analytics HTML report.
 * Thread Safety:
 *   - AtomicInteger counters for pass/fail/skip (no race on increment)
 *   - CopyOnWriteArrayList for run history (concurrent appends are safe)
 *   - ConcurrentHashMap for per-test metrics and flaky detection
 *   - No ThreadLocal needed here — we want shared aggregation across threads
 */
public class SmartAnalyticsReporter {

    private static final AtomicInteger totalTests   = new AtomicInteger(0);
    private static final AtomicInteger passedTests  = new AtomicInteger(0);
    private static final AtomicInteger failedTests  = new AtomicInteger(0);
    private static final AtomicInteger skippedTests = new AtomicInteger(0);
    private static final long suiteStartMs          = System.currentTimeMillis();

    // Thread-safe collections
    private static final CopyOnWriteArrayList<TestRunRecord> runHistory   = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, TestMetrics> metricsMap = new ConcurrentHashMap<>();

    private SmartAnalyticsReporter() {}

    public static void record(ITestResult result) {
        String name     = result.getMethod().getMethodName();
        long   duration = result.getEndMillis() - result.getStartMillis();
        int    status   = result.getStatus();
        int    healings = HealingCache.getEvents().stream()
                .filter(e -> e.threadName().equals(Thread.currentThread().getName()))
                .mapToInt(e -> 1).sum();

        totalTests.incrementAndGet();
        switch (status) {
            case ITestResult.SUCCESS -> passedTests.incrementAndGet();
            case ITestResult.FAILURE -> failedTests.incrementAndGet();
            case ITestResult.SKIP    -> skippedTests.incrementAndGet();
        }

        runHistory.add(new TestRunRecord(
            name, status, duration, healings,
            result.getThrowable() != null ? result.getThrowable().getMessage() : "",
            Thread.currentThread().getName()
        ));

        metricsMap.compute(name, (k, existing) -> {
            TestMetrics m = existing != null ? existing : new TestMetrics(name);
            m.addRun(status == ITestResult.SUCCESS, duration);
            return m;
        });
    }

    public static void generateReport() {
        try {
            String dir  = "test-output/analytics/";
            Files.createDirectories(Paths.get(dir));
            String path = dir + "analytics_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".html";
            Files.writeString(Paths.get(path), buildHtml());
            LoggerUtil.info("[Analytics] Report generated: " + path);
        } catch (IOException e) {
            LoggerUtil.error("[Analytics] Report generation failed: " + e.getMessage());
        }
    }

    // ── HTML builder ──────────────────────────────────────────

    private static String buildHtml() {
        int    total    = totalTests.get();
        int    passed   = passedTests.get();
        int    failed   = failedTests.get();
        int    skipped  = skippedTests.get();
        double passRate = total > 0 ? passed * 100.0 / total : 0;
        long   elapsed  = (System.currentTimeMillis() - suiteStartMs) / 1000;
        int    healed   = HealingCache.getHealedCount();

        List<String> flakyTests = detectFlaky();
        List<String> slowTests  = detectSlow();
        List<String> insights   = buildInsights(passRate, flakyTests, slowTests, healed);

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>AI Test Analytics</title>
              <style>
                *{margin:0;padding:0;box-sizing:border-box}
                body{font-family:'Segoe UI',sans-serif;background:#0f172a;color:#e2e8f0}
                .header{background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:28px;text-align:center}
                .header h1{font-size:1.8rem;margin-bottom:6px}
                .header p{opacity:.8;font-size:.9rem}
                .container{max-width:1100px;margin:24px auto;padding:0 16px}
                .grid{display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-bottom:24px}
                .card{background:#1e293b;border-radius:10px;padding:16px;text-align:center;border:1px solid #334155}
                .card .val{font-size:2rem;font-weight:700;margin:6px 0}
                .card .lbl{font-size:.7rem;color:#94a3b8;text-transform:uppercase;letter-spacing:1px}
                .green{color:#22c55e} .red{color:#ef4444} .yellow{color:#f59e0b}
                .blue{color:#06b6d4}  .purple{color:#a78bfa}
                .section{background:#1e293b;border-radius:10px;padding:20px;margin-bottom:20px;border:1px solid #334155}
                .section h2{font-size:1.1rem;margin-bottom:14px;color:#a78bfa}
                .bar-bg{background:#334155;border-radius:100px;height:10px;margin:8px 0}
                .bar{height:100%%;border-radius:100px;background:linear-gradient(90deg,#22c55e,#86efac)}
                .insight{background:#0f172a;border-left:3px solid #6366f1;padding:10px 14px;margin:6px 0;border-radius:0 8px 8px 0;font-size:.88rem}
                table{width:100%%;border-collapse:collapse;font-size:.82rem}
                th{background:#334155;padding:9px 12px;text-align:left;font-weight:600}
                td{padding:9px 12px;border-bottom:1px solid #1e293b}
                .badge{display:inline-block;padding:2px 9px;border-radius:100px;font-size:.72rem;font-weight:700}
                .bp{background:#052e16;color:#22c55e} .bf{background:#450a0a;color:#ef4444}
                .bs{background:#451a03;color:#f59e0b} .bh{background:#083344;color:#06b6d4}
                .fl{background:#4c1d95;color:#c4b5fd;font-size:.68rem;padding:2px 7px;border-radius:100px;margin-left:5px}
                footer{text-align:center;padding:16px;color:#475569;font-size:.78rem}
              </style>
            </head>
            <body>
              <div class="header">
                <h1>🤖 AI Test Analytics Dashboard</h1>
                <p>%s &nbsp;·&nbsp; Suite duration: %ss &nbsp;·&nbsp; Threads: parallel</p>
              </div>
              <div class="container">
                <div class="grid">
                  <div class="card"><div class="lbl">Total</div><div class="val purple">%d</div></div>
                  <div class="card"><div class="lbl">Passed</div><div class="val green">%d</div></div>
                  <div class="card"><div class="lbl">Failed</div><div class="val red">%d</div></div>
                  <div class="card"><div class="lbl">Skipped</div><div class="val yellow">%d</div></div>
                  <div class="card"><div class="lbl">Healed</div><div class="val blue">%d</div></div>
                </div>
                <div class="section">
                  <h2>📊 Pass Rate</h2>
                  <p style="margin-bottom:6px">%.1f%% of tests passed</p>
                  <div class="bar-bg"><div class="bar" style="width:%.1f%%"></div></div>
                </div>
                <div class="section">
                  <h2>🧠 AI Insights</h2>%s
                </div>
                <div class="section">
                  <h2>📋 Test Results</h2>
                  <table><thead><tr>
                    <th>Test</th><th>Status</th><th>Duration</th><th>Healings</th><th>Thread</th>
                  </tr></thead><tbody>%s</tbody></table>
                </div>
              </div>
              <footer>AI Mobile Automation Framework &nbsp;·&nbsp; Self-Healing + Visual AI + Smart Analytics</footer>
            </body></html>
            """.formatted(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                elapsed, total, passed, failed, skipped, healed,
                passRate, Math.min(passRate, 100.0),
                insights.stream().map(i -> "<div class='insight'>" + i + "</div>").collect(Collectors.joining()),
                buildRows(flakyTests)
            );
    }

    private static String buildRows(List<String> flakyTests) {
        return runHistory.stream().map(r -> {
            String badge = switch (r.status) {
                case ITestResult.SUCCESS -> "<span class='badge bp'>PASS</span>";
                case ITestResult.FAILURE -> "<span class='badge bf'>FAIL</span>";
                default                  -> "<span class='badge bs'>SKIP</span>";
            };
            String flaky  = flakyTests.contains(r.name) ? "<span class='fl'>FLAKY</span>" : "";
            String healed = r.healings > 0 ? "<span class='badge bh'>" + r.healings + "×</span>" : "—";
            return "<tr><td>" + r.name + flaky + "</td><td>" + badge + "</td><td>"
                    + r.duration + "ms</td><td>" + healed + "</td><td>" + r.thread + "</td></tr>";
        }).collect(Collectors.joining());
    }

    private static List<String> detectFlaky() {
        return metricsMap.entrySet().stream()
                .filter(e -> e.getValue().isFlaky())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static List<String> detectSlow() {
        return runHistory.stream()
                .filter(r -> r.duration > 30_000)
                .map(r -> r.name)
                .distinct()
                .collect(Collectors.toList());
    }

    private static List<String> buildInsights(double passRate, List<String> flaky,
                                               List<String> slow, int healed) {
        List<String> out = new ArrayList<>();
        if      (passRate == 100) out.add("✅ Perfect run — all tests passed!");
        else if (passRate >= 90)  out.add("✅ Excellent pass rate: " + String.format("%.1f%%", passRate));
        else if (passRate >= 75)  out.add("⚠️ Pass rate " + String.format("%.1f%%", passRate) + " — review failures.");
        else                      out.add("🚨 Low pass rate: " + String.format("%.1f%%", passRate) + " — immediate action needed.");

        if (!flaky.isEmpty()) out.add("⚠️ Flaky tests detected: " + String.join(", ", flaky));
        if (!slow.isEmpty())  out.add("🐢 Slow tests (>30s): " + String.join(", ", slow));
        if (healed > 0)       out.add("🔧 " + healed + " locator(s) were self-healed. Consider updating YAML locator files.");
        return out.isEmpty() ? List.of("ℹ️ No specific insights for this run.") : out;
    }

    // ── Value objects ─────────────────────────────────────────

    private record TestRunRecord(String name, int status, long duration,
                                  int healings, String error, String thread) {}

    private static class TestMetrics {
        final String name;
        int runs = 0, passes = 0;
        TestMetrics(String name) { this.name = name; }
        synchronized void addRun(boolean passed, long duration) {
            runs++; if (passed) passes++;
        }
        boolean isFlaky() { return runs > 1 && passes > 0 && passes < runs; }
    }
}
