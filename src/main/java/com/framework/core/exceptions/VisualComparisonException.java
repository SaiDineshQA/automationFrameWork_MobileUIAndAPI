package com.framework.core.exceptions;

public class VisualComparisonException extends FrameworkException {
    public VisualComparisonException(String testName, Throwable cause) {
        super("Visual comparison failed for checkpoint: " + testName, cause);
    }
}
