package com.framework.core.interfaces;

import com.framework.core.config.YamlLocatorRepository;
import org.openqa.selenium.By;

import java.util.List;

/**
 * ILocatorRepository - Contract for locator loading strategies.
 * Open/Closed Principle: new formats (XML, DB) add new implementations, not edits.
 */
public interface ILocatorRepository {
    By getLocator(String pageName, String elementName, String platform);
    List<YamlLocatorRepository.LocatorStrategy> getAllStrategies(String pageName, String elementName, String platform);
    void reload(String pageName);
}
