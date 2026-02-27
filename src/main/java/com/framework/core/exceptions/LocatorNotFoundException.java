package com.framework.core.exceptions;

public class LocatorNotFoundException extends FrameworkException {
    public LocatorNotFoundException(String page, String element) {
        super(String.format("Locator not found for element '%s' on page '%s'. " +
              "Check src/main/resources/locators/%s.yaml", element, page, page));
    }
}
