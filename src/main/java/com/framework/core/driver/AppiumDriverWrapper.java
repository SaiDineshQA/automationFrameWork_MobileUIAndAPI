package com.framework.core.driver;

import com.framework.core.interfaces.IDriver;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * AppiumDriverWrapper - Concrete IDriver implementation wrapping AppiumDriver.
 *
 * Single Responsibility: wraps driver operations only.
 * Liskov Substitution: can be used anywhere IDriver is expected.
 */
public class AppiumDriverWrapper implements IDriver {

    private final AppiumDriver driver;
    private final String platform;

    public AppiumDriverWrapper(AppiumDriver driver, String platform) {
        this.driver   = driver;
        this.platform = platform;
    }

    @Override
    public WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    @Override
    public List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    @Override
    public void quit() {
        driver.quit();
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    @Override
    public boolean isAndroid() {
        return driver instanceof AndroidDriver;
    }

    @Override
    public boolean isIOS() {
        return driver instanceof IOSDriver;
    }

    @Override
    public Object getUnderlyingDriver() {
        return driver;
    }
}
