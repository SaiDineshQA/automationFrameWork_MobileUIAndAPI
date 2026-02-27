package com.framework.core.interfaces;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

/**
 * IDriver - Interface abstracting driver operations.
 * Follows Interface Segregation Principle (ISP).
 * Allows swapping driver implementations without changing test code.
 */
public interface IDriver {
    WebElement findElement(By locator);
    List<WebElement> findElements(By locator);
    void quit();
    String getPlatform();
    boolean isAndroid();
    boolean isIOS();
    Object getUnderlyingDriver();
}
