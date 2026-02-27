package com.framework.core.exceptions;

public class DriverInitException extends FrameworkException {
    public DriverInitException(String platform, Throwable cause) {
        super("Failed to initialize driver for platform: " + platform, cause);
    }
}
