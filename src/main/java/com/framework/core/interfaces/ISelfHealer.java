package com.framework.core.interfaces;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

/**
 * ISelfHealer - Contract for self-healing element strategies.
 * Open/Closed: add new healing strategies without touching existing ones.
 */
public interface ISelfHealer {
    WebElement heal(By failedLocator, String elementName, IDriver driver);
    WebElement heal(List<By> failedLocators, String elementName, IDriver driver);
    List<String> getHealingLog();
    void clearLog();
}
