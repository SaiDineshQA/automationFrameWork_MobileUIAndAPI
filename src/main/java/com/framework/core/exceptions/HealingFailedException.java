package com.framework.core.exceptions;

public class HealingFailedException extends FrameworkException {
    public HealingFailedException(String elementName, String originalLocator) {
        super(String.format("Self-healing exhausted all strategies for element '%s'. " +
              "Original locator: %s. Update the YAML locator file.", elementName, originalLocator));
    }
}
