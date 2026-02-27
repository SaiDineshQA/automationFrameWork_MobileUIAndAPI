package com.framework.core.exceptions;

/** Base exception for all framework errors */
public class FrameworkException extends RuntimeException {
    public FrameworkException(String message) { super(message); }
    public FrameworkException(String message, Throwable cause) { super(message, cause); }
}
