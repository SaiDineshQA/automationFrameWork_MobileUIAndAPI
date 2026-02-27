package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** LoggerUtil - thin wrapper. Thread name auto-included by Log4j2 pattern. */
public final class LoggerUtil {
    private static final Logger log = LogManager.getLogger(LoggerUtil.class);
    private LoggerUtil() {}
    public static void info(String msg)  { log.info(msg);  }
    public static void warn(String msg)  { log.warn(msg);  }
    public static void error(String msg) { log.error(msg); }
    public static void debug(String msg) { log.debug(msg); }
}
