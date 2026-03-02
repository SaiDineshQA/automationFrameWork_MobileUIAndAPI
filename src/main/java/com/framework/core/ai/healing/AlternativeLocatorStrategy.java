package com.framework.core.ai.healing;

import com.framework.utils.LoggerUtil;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AlternativeLocatorStrategy - Generates intelligent fallback locators from a failed one.
 *
 * Parses the failed locator to extract meaningful values (ids, text, content-desc, etc.)
 * then generates alternatives using those values across different attribute types.
 *
 * Example:
 *   Failed: By.id("com.app:id/findStoreButton")
 *   Extracts: "com.app:id/findStoreButton", "findStoreButton"
 *   Generates: //*[@resource-id='com.app:id/findStoreButton']
 *              //*[contains(@resource-id,'findStoreButton')]
 *              //*[@content-desc='findStoreButton']
 *              //*[@text='findStoreButton']  etc.
 *
 *   Failed: By.xpath("//*[@text='Find Store']")
 *   Extracts: "Find Store"
 *   Generates: //*[@text='Find Store']
 *              //*[contains(@text,'Find Store')]
 *              //*[@content-desc='Find Store']
 *              //*[@name='Find Store']
 *              By.id("Find Store")  etc.
 */
public class AlternativeLocatorStrategy {

    // Patterns to extract attribute values from xpaths
    // Matches: @attribute='value' or @attribute="value"
    private static final Pattern XPATH_ATTR_PATTERN =
            Pattern.compile("@([\\w-]+)\\s*=\\s*['\"]([^'\"]+)['\"]");

    // Matches: contains(@attribute,'value') or contains(@attribute,"value")
    private static final Pattern XPATH_CONTAINS_PATTERN =
            Pattern.compile("contains\\s*\\(\\s*@([\\w-]+)\\s*,\\s*['\"]([^'\"]+)['\"]\\s*\\)");

    private AlternativeLocatorStrategy() {}

    /**
     * Generates ordered list of alternative By locators to try during healing.
     * Uses LinkedHashSet to avoid duplicate locators while preserving order.
     */
    public static List<By> generate(By failedLocator, String elementName) {
        Set<String> seen = new LinkedHashSet<>(); // dedup by toString()
        List<By> alternatives = new ArrayList<>();

        String locatorStr = failedLocator.toString(); // e.g. "By.id: someId" or "By.xpath: //..."
        String locatorType = extractType(locatorStr);
        String rawValue = extractRawValue(locatorStr);

        // Step 1: Extract meaningful values from the failed locator
        List<String> extractedValues = extractMeaningfulValues(locatorType, rawValue);

        LoggerUtil.debug("[Healing] Locator type: " + locatorType
                + ", extracted values: " + extractedValues);

        // Step 2: Generate alternatives from each extracted value
        for (String value : extractedValues) {
            addAlternative(alternatives, seen, By.id(value));
            addAlternative(alternatives, seen, AppiumBy.accessibilityId(value));
            addAlternative(alternatives, seen, By.xpath("//*[@resource-id='" + value + "']"));
            addAlternative(alternatives, seen,
                    By.xpath("//*[contains(@resource-id,'" + value + "')]"));
            addAlternative(alternatives, seen, By.xpath("//*[@content-desc='" + value + "']"));
            addAlternative(alternatives, seen, By.xpath("//*[@name='" + value + "']"));
            addAlternative(alternatives, seen, By.xpath("//*[@label='" + value + "']"));
            addAlternative(alternatives, seen, By.xpath("//*[@text='" + value + "']"));
            addAlternative(alternatives, seen,
                    By.xpath("//*[contains(@text,'" + value + "')]"));
        }

        // Step 3: Add elementName-derived locators (camelCase → snake_case, kebab-case)
        if (elementName != null && !elementName.isEmpty()) {
            addNameDerivedLocators(alternatives, seen, elementName);
        }

        return alternatives;
    }

    /**
     * Extracts meaningful, plain-text values from a locator based on its type.
     * These values are then used to generate alternatives across different attributes.
     */
    private static List<String> extractMeaningfulValues(String type, String rawValue) {
        Set<String> values = new LinkedHashSet<>(); // preserve insertion order, no dupes

        if (rawValue == null || rawValue.isEmpty()) return new ArrayList<>(values);

        switch (type) {
            case "id" -> {
                // rawValue = "com.app:id/findStoreButton"
                values.add(rawValue);
                // Also extract the short id after the last /
                if (rawValue.contains("/")) {
                    values.add(rawValue.substring(rawValue.lastIndexOf('/') + 1));
                }
                // Also extract after : for "package:id/name" format
                if (rawValue.contains(":id/")) {
                    values.add(rawValue.substring(rawValue.indexOf(":id/") + 4));
                }
            }
            case "xpath" -> {
                // Parse xpath to extract attribute values
                // e.g. //*[@text='Find Store'] → "Find Store"
                // e.g. //*[@resource-id='com.app:id/btn'] → "com.app:id/btn", "btn"
                extractXPathAttributeValues(rawValue, values);
            }
            case "css" -> {
                // Parse CSS selectors: #id, .class, [attr='val']
                extractCssValues(rawValue, values);
            }
            case "accessibility", "accessibilityid" -> {
                values.add(rawValue);
            }
            case "classname", "name", "tagname" -> {
                values.add(rawValue);
            }
            default -> {
                values.add(rawValue);
            }
        }

        return new ArrayList<>(values);
    }

    /**
     * Parses xpath expressions and extracts actual attribute values.
     * Handles: @attr='value', contains(@attr,'value')
     */
    private static void extractXPathAttributeValues(String xpath, Set<String> values) {
        // Match @attribute='value'
        Matcher attrMatcher = XPATH_ATTR_PATTERN.matcher(xpath);
        while (attrMatcher.find()) {
            String attrValue = attrMatcher.group(2);
            values.add(attrValue);
            // If it's a resource-id, also extract the short part
            if (attrValue.contains("/")) {
                values.add(attrValue.substring(attrValue.lastIndexOf('/') + 1));
            }
            if (attrValue.contains(":id/")) {
                values.add(attrValue.substring(attrValue.indexOf(":id/") + 4));
            }
        }

        // Match contains(@attribute,'value')
        Matcher containsMatcher = XPATH_CONTAINS_PATTERN.matcher(xpath);
        while (containsMatcher.find()) {
            String attrValue = containsMatcher.group(2);
            values.add(attrValue);
        }
    }

    /**
     * Parses CSS selectors and extracts meaningful values.
     * Handles: #id, [attr='val'], [content-desc='val']
     */
    private static void extractCssValues(String css, Set<String> values) {
        // #someId → someId
        Pattern idPattern = Pattern.compile("#([\\w-]+)");
        Matcher idMatcher = idPattern.matcher(css);
        while (idMatcher.find()) {
            values.add(idMatcher.group(1));
        }

        // [attr='value'] or [attr="value"]
        Pattern attrPattern = Pattern.compile("\\[([\\w-]+)=['\"]([^'\"]+)['\"]\\]");
        Matcher attrMatcher = attrPattern.matcher(css);
        while (attrMatcher.find()) {
            values.add(attrMatcher.group(2));
        }
    }

    /**
     * Generates locators derived from the element name (camelCase conversions).
     */
    private static void addNameDerivedLocators(List<By> list, Set<String> seen, String elementName) {
        String snake = camelToSnake(elementName);
        String kebab = snake.replace("_", "-");
        String humanText = camelToWords(elementName);

        addAlternative(list, seen,
                By.xpath("//*[contains(@resource-id,'" + snake + "')]"));
        addAlternative(list, seen,
                By.xpath("//*[contains(@resource-id,'" + kebab + "')]"));
        addAlternative(list, seen,
                By.xpath("//*[contains(@content-desc,'" + elementName + "')]"));
        addAlternative(list, seen,
                By.xpath("//*[contains(translate(@text,"
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
                        + "'" + humanText.toLowerCase() + "')]"));
    }

    /**
     * Adds a locator to the list only if it hasn't been seen before (dedup).
     */
    private static void addAlternative(List<By> list, Set<String> seen, By locator) {
        String key = locator.toString();
        if (seen.add(key)) {
            list.add(locator);
        }
    }

    /**
     * Extracts the locator type from By.toString().
     * "By.id: value"    → "id"
     * "By.xpath: value"  → "xpath"
     * "By.cssSelector: value" → "css"
     */
    private static String extractType(String locatorString) {
        // Selenium By patterns
        if (locatorString.startsWith("By.id:")) return "id";
        if (locatorString.startsWith("By.xpath:")) return "xpath";
        if (locatorString.startsWith("By.cssSelector:")) return "css";
        if (locatorString.startsWith("By.className:")) return "classname";
        if (locatorString.startsWith("By.name:")) return "name";
        if (locatorString.startsWith("By.tagName:")) return "tagname";
        // AppiumBy patterns
        if (locatorString.contains("accessibilityId:") || locatorString.contains("accessibility id:")) return "accessibilityid";
        if (locatorString.contains("androidUIAutomator:") || locatorString.contains("uiautomator:")) return "androiduiautomator";
        if (locatorString.contains("iOSClassChain:") || locatorString.contains("classChain:")) return "iosclasschain";
        if (locatorString.contains("iOSNsPredicate:") || locatorString.contains("predicate string:")) return "iosnspredicate";
        return "unknown";
    }

    /**
     * Extracts the raw value portion from By.toString().
     * "By.id: com.app:id/btn" → "com.app:id/btn"
     * "By.xpath: //*[@text='Find Store']" → "//*[@text='Find Store']"
     */
    private static String extractRawValue(String locatorString) {
        int colonSpace = locatorString.indexOf(": ");
        if (colonSpace >= 0 && colonSpace + 2 < locatorString.length()) {
            return locatorString.substring(colonSpace + 2).trim();
        }
        return "";
    }

    private static String camelToSnake(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private static String camelToWords(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
