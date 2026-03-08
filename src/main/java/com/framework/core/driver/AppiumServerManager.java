package com.framework.core.driver;

import com.framework.core.config.ConfigManager;
import com.framework.utils.LoggerUtil;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * AppiumServerManager - Manages a single shared Appium server for all test threads.
 *
 * <h3>Architecture:</h3>
 * <pre>
 *   ┌──────────────────────────────────────────────────┐
 *   │         ONE Appium Server (port 4723)             │
 *   │                                                    │
 *   │  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
 *   │  │ Session 1 │  │ Session 2 │  │ Session 3 │       │
 *   │  │ (Thread-1)│  │ (Thread-2)│  │ (Thread-3)│       │
 *   │  │ Device A  │  │ Device B  │  │ Device C  │       │
 *   │  └──────────┘  └──────────┘  └──────────┘       │
 *   └──────────────────────────────────────────────────┘
 * </pre>
 *
 * <ul>
 *   <li><b>Single server</b> — one Appium process, one port, shared by all threads</li>
 *   <li><b>Multiple sessions</b> — each thread creates its own driver session (isolated via ThreadLocal in DriverManager)</li>
 *   <li><b>Parallel safe</b> — Appium 2.x natively supports concurrent sessions on one server</li>
 *   <li><b>Cross-platform</b> — works on macOS, Windows, and Linux (auto-detects appium from PATH)</li>
 *   <li><b>Skipped for remote/cloud</b> — Sauce Labs, BrowserStack, etc. use their own server URL</li>
 * </ul>
 *
 * <h3>Configuration (config.properties):</h3>
 * <pre>
 *   appium.auto.start=true          # Enable/disable auto-start (default: false)
 *   appium.base.port=4723           # Port for the Appium server
 *   appium.startup.timeout=120      # Max seconds to wait for server to start
 *   appium.path=                    # Custom appium JS path (empty = auto-detect from PATH)
 * </pre>
 *
 * <h3>Lifecycle (managed by BaseTest):</h3>
 * <pre>
 *   @BeforeSuite  → AppiumServerManager.start()     // starts one server
 *   @BeforeMethod → deviceConfig.setServerUrl(...)   // all threads use same URL
 *   @AfterSuite   → AppiumServerManager.stop()      // stops the one server
 * </pre>
 */
public final class AppiumServerManager {

    /** The single shared Appium server instance. */
    private static volatile AppiumDriverLocalService service;

    /** The port the server is running on. */
    private static volatile int serverPort;

    /** Lock object for thread-safe server start/stop. */
    private static final Object LOCK = new Object();

    /** Tracks whether the shutdown hook has been registered. */
    private static volatile boolean shutdownHookRegistered = false;

    private AppiumServerManager() {}

    // ── Public API ────────────────────────────────────────────

    /**
     * Checks if Appium auto-start is enabled AND execution is local.
     *
     * @param environment "local" or "remote"
     * @return true if auto-start should be used
     */
    public static boolean isEnabled(String environment) {
        return ConfigManager.getBoolean("appium.auto.start", false)
                && "local".equalsIgnoreCase(environment);
    }

    /**
     * Starts the shared Appium server (if not already running).
     * Thread-safe — only the first caller starts the server; subsequent calls are no-ops.
     *
     * Called once in {@code @BeforeSuite}.
     */
    public static void start() {
        if (isRunning()) {
            LoggerUtil.info("[AppiumServer] Already running on port " + serverPort
                    + " → " + service.getUrl());
            return;
        }

        synchronized (LOCK) {
            // Double-checked locking — another thread may have started it while we waited
            if (isRunning()) return;

            int port = ConfigManager.getInt("appium.base.port", 4723);

            // Find a free port starting from the configured base port
            port = findAvailablePort(port);
            serverPort = port;

            LoggerUtil.info("[AppiumServer] Starting Appium server on port " + port + "...");

            AppiumServiceBuilder builder = new AppiumServiceBuilder()
                    .withIPAddress("127.0.0.1")
                    .usingPort(port)
                    .withTimeout(Duration.ofSeconds(
                            ConfigManager.getInt("appium.startup.timeout", 120)
                    ));

            // Custom appium path (if configured)
            String appiumPath = ConfigManager.get("appium.path", "").trim();
            if (!appiumPath.isEmpty()) {
                builder.withAppiumJS(new File(appiumPath));
            }

            // Ensure node/npm/ANDROID_HOME are in PATH (macOS + Windows)
            configureEnvironment(builder);

            service = AppiumDriverLocalService.buildService(builder);
            service.start();

            // Register JVM shutdown hook — guarantees cleanup even on abrupt termination
            // (Ctrl+C, IDE stop button, OOM, System.exit)
            registerShutdownHook();

            LoggerUtil.info("[AppiumServer] ✅ Started → " + service.getUrl());
        }
    }

    /**
     * Returns the URL of the running Appium server.
     *
     * @return server URL (e.g., http://127.0.0.1:4723)
     * @throws IllegalStateException if the server is not running
     */
    public static URL getUrl() {
        if (!isRunning()) {
            throw new IllegalStateException(
                    "[AppiumServer] Server is not running. Ensure appium.auto.start=true "
                            + "and environment=local.");
        }
        return service.getUrl();
    }

    /**
     * Returns the port the server is running on.
     */
    public static int getPort() {
        return serverPort;
    }

    /**
     * Checks if the Appium server is currently running.
     */
    public static boolean isRunning() {
        return service != null && service.isRunning();
    }

    /**
     * Stops the shared Appium server and ensures the port is freed.
     * Thread-safe and idempotent — safe to call multiple times.
     *
     * Cleanup order:
     *   1. Graceful stop via AppiumDriverLocalService.stop()
     *   2. Force-kill any process still holding the port (fallback)
     *   3. Verify the port is actually freed
     *
     * Called once in {@code @AfterSuite} and also by JVM shutdown hook.
     */
    public static void stop() {
        synchronized (LOCK) {
            if (service == null && serverPort == 0) return;

            int port = serverPort;

            // Step 1: Graceful stop
            try {
                if (service != null && service.isRunning()) {
                    service.stop();
                    LoggerUtil.info("[AppiumServer] Graceful stop on port " + port);
                }
            } catch (Exception e) {
                LoggerUtil.warn("[AppiumServer] Graceful stop failed: " + e.getMessage());
            }

            // Step 2: Force-kill anything still on the port (handles zombie processes)
            if (port > 0) {
                forceKillPort(port);
            }

            // Step 3: Verify port is freed
            if (port > 0) {
                try (ServerSocket probe = new ServerSocket(port)) {
                    probe.setReuseAddress(true);
                    LoggerUtil.info("[AppiumServer] ✅ Port " + port + " is free");
                } catch (IOException e) {
                    LoggerUtil.warn("[AppiumServer] ⚠ Port " + port + " may still be occupied");
                }
            }

            service = null;
            serverPort = 0;
        }
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * Registers a JVM shutdown hook to stop the Appium server on abrupt termination.
     * Handles: Ctrl+C, IDE stop button, OOM crash, System.exit().
     * Registered only once (idempotent).
     */
    private static void registerShutdownHook() {
        if (shutdownHookRegistered) return;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isRunning()) {
                System.out.println("[AppiumServer] JVM shutting down — stopping Appium server...");
                stop();
            }
        }, "appium-shutdown-hook"));
        shutdownHookRegistered = true;
    }

    /**
     * Force-kills any process listening on the given port.
     * Works on macOS/Linux (lsof + kill) and Windows (PowerShell + Stop-Process).
     */
    private static void forceKillPort(int port) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                // Windows: PowerShell — reliable across all Windows versions
                String psCommand = String.format(
                        "Get-NetTCPConnection -LocalPort %d -ErrorAction SilentlyContinue | " +
                        "ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }",
                        port);
                pb = new ProcessBuilder("powershell", "-Command", psCommand);
            } else {
                // macOS / Linux: lsof → kill
                pb = new ProcessBuilder("sh", "-c",
                        "lsof -ti:" + port + " | xargs kill -9 2>/dev/null");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            process.destroyForcibly();

            LoggerUtil.debug("[AppiumServer] Force-kill attempted on port " + port);
        } catch (Exception e) {
            LoggerUtil.debug("[AppiumServer] Force-kill skipped: " + e.getMessage());
        }
    }

    /**
     * Finds an available port starting from the given port.
     * Probes with ServerSocket — skips occupied ports.
     */
    private static int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 100; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                socket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                LoggerUtil.debug("[AppiumServer] Port " + port + " occupied, trying next...");
            }
        }
        throw new RuntimeException("[AppiumServer] No available port found in range "
                + startPort + "-" + (startPort + 99));
    }

    /**
     * Configures environment variables for the Appium process.
     *
     * On macOS: node/npm may not be in the default PATH when launched from an IDE.
     * Adds common node installation paths (Homebrew, nvm, volta, fnm).
     *
     * On Windows: adds common npm/node/Volta paths.
     *
     * Both: sets ANDROID_HOME / ANDROID_SDK_ROOT.
     */
    private static void configureEnvironment(AppiumServiceBuilder builder) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String existingPath = System.getenv("PATH");
        String home = System.getProperty("user.home");

        String androidHome = resolveAndroidHome(home, os);

        if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            String nodePaths = String.join(":",
                    "/opt/homebrew/bin",
                    "/usr/local/bin",
                    home + "/.nvm/versions/node/default/bin",
                    home + "/.volta/bin",
                    home + "/.fnm/aliases/default/bin",
                    "/usr/local/lib/node_modules/.bin",
                    existingPath != null ? existingPath : ""
            );

            builder.withEnvironment(Map.of(
                    "PATH", nodePaths,
                    "ANDROID_HOME", androidHome,
                    "ANDROID_SDK_ROOT", androidHome
            ));

        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            String localAppData = System.getenv("LOCALAPPDATA");
            String programFiles = System.getenv("ProgramFiles");

            String nodePaths = String.join(";",
                    appData != null ? appData + "\\npm" : "",
                    localAppData != null ? localAppData + "\\Programs\\Volta\\bin" : "",
                    programFiles != null ? programFiles + "\\nodejs" : "",
                    home + "\\.nvm\\versions\\node\\default",
                    existingPath != null ? existingPath : ""
            );

            builder.withEnvironment(Map.of(
                    "PATH", nodePaths,
                    "ANDROID_HOME", androidHome,
                    "ANDROID_SDK_ROOT", androidHome
            ));
        }
    }

    /**
     * Resolves ANDROID_HOME from env variables or defaults.
     */
    private static String resolveAndroidHome(String home, String os) {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null && !androidHome.isEmpty()) return androidHome;

        androidHome = System.getenv("ANDROID_SDK_ROOT");
        if (androidHome != null && !androidHome.isEmpty()) return androidHome;

        // Default paths
        if (os.contains("win")) {
            return home + "\\AppData\\Local\\Android\\Sdk";
        }
        return home + "/Library/Android/sdk";
    }
}

