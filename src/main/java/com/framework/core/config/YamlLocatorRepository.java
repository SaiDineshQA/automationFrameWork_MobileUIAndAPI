package com.framework.core.config;

import com.framework.core.exceptions.LocatorNotFoundException;
import com.framework.core.interfaces.ILocatorRepository;
import com.framework.utils.LoggerUtil;
import org.openqa.selenium.By;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YamlLocatorRepository - Loads element locators from YAML files.
 *
 * Single Responsibility: locator loading and caching only.
 *
 * NEW FORMAT with OR conditions:
 *
 *   cartIcon:
 *     android: xpath:://android.widget.ImageView[@content-desc='cart'] | css::[content-desc='cart']
 *     ios: id::cart_button | accessibility::cart_button
 *
 *   loginButton:
 *     common: id::login_btn | xpath:://button[@id='login_btn']
 *
 * Format: <type>::<value> | <type>::<value> | ...
 *
 * Supported types: id | xpath | css | accessibility | classname | name | tagname
 *
 * OR logic (|):
 * - Framework tries locators left-to-right
 * - If xpath fails → tries css → tries next strategy
 * - First successful locator is used
 *
 * Thread-safe: ConcurrentHashMap cache, safe for parallel execution.
 */
public class YamlLocatorRepository implements ILocatorRepository {

    // pageName -> elementName -> platform -> List<LocatorStrategy>
    private static final Map<String, Map<String, Map<String, List<LocatorStrategy>>>> cache
            = new ConcurrentHashMap<>();

    private static final String LOCATOR_BASE_PATH = "locators/";
    private static final YamlLocatorRepository INSTANCE = new YamlLocatorRepository();

    // Regex to parse: type::value
    private static final Pattern LOCATOR_PATTERN = Pattern.compile("(\\w+)::(.+)");

    private YamlLocatorRepository() {}

    public static YamlLocatorRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public By getLocator(String pageName, String elementName, String platform) {
        ensureLoaded(pageName);

        Map<String, Map<String, List<LocatorStrategy>>> pageLocators = cache.get(pageName);
        if (pageLocators == null || !pageLocators.containsKey(elementName)) {
            throw new LocatorNotFoundException(pageName, elementName);
        }

        Map<String, List<LocatorStrategy>> platformMap = pageLocators.get(elementName);
        List<LocatorStrategy> strategies = platformMap.getOrDefault(
                platform.toLowerCase(),
                platformMap.get("common") // fallback to 'common' if platform-specific not found
        );

        if (strategies == null || strategies.isEmpty()) {
            throw new LocatorNotFoundException(pageName,
                    elementName + " [platform: " + platform + "]");
        }

        // Return the primary (first) strategy as By
        // OR conditions are handled by ElementFinder (self-healing logic)
        LocatorStrategy primary = strategies.get(0);
        return buildBy(primary.type, primary.value, elementName);
    }

    /**
     * Get all strategies for an element (for OR condition support in self-healing).
     * Returns list of locator strategies in priority order.
     */
    public List<LocatorStrategy> getAllStrategies(String pageName, String elementName, String platform) {
        ensureLoaded(pageName);

        Map<String, Map<String, List<LocatorStrategy>>> pageLocators = cache.get(pageName);
        if (pageLocators == null || !pageLocators.containsKey(elementName)) {
            return Collections.emptyList();
        }

        Map<String, List<LocatorStrategy>> platformMap = pageLocators.get(elementName);
        List<LocatorStrategy> strategies = platformMap.getOrDefault(
                platform.toLowerCase(),
                platformMap.get("common")
        );

        return strategies != null ? new ArrayList<>(strategies) : Collections.emptyList();
    }

    @Override
    public void reload(String pageName) {
        cache.remove(pageName);
        ensureLoaded(pageName);
    }

    private void ensureLoaded(String pageName) {
        cache.computeIfAbsent(pageName, this::parseYaml);
    }

    /**
     * Parses YAML with new expression format:
     *
     * elementName:
     *   platform: type::value | type::value | ...
     *
     * Example:
     *   cartIcon:
     *     android: xpath:://div[@id='cart'] | css::#cart
     *     ios: id::cart_button
     */
    private Map<String, Map<String, List<LocatorStrategy>>> parseYaml(String pageName) {
        String filePath = LOCATOR_BASE_PATH + pageName + ".yaml";
        Map<String, Map<String, List<LocatorStrategy>>> result = new LinkedHashMap<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                LoggerUtil.warn("[Locators] YAML file not found: " + filePath);
                return result;
            }

            Scanner scanner = new Scanner(is);
            String currentElement = null;

            while (scanner.hasNextLine()) {
                String raw = scanner.nextLine();
                if (raw.trim().isEmpty() || raw.trim().startsWith("#")) continue;

                int indent = countLeadingSpaces(raw);
                String line = raw.trim();

                if (indent == 0 && line.endsWith(":")) {
                    // Top-level: element name
                    currentElement = line.substring(0, line.length() - 1);
                    result.put(currentElement, new LinkedHashMap<>());

                } else if (indent == 2 && currentElement != null && line.contains(":")) {
                    // Second-level: platform and expression
                    // Format: "android: xpath::value | css::value"
                    int colonIdx = line.indexOf(':');
                    if (colonIdx > 0) {
                        String platformKey = line.substring(0, colonIdx).trim();
                        String expression = line.substring(colonIdx + 1).trim();

                        // Parse expression into list of strategies
                        List<LocatorStrategy> strategies = parseExpression(expression, currentElement);
                        result.get(currentElement).put(platformKey, strategies);
                    }
                }
            }

            LoggerUtil.info("[Locators] Loaded: " + filePath + " (" + result.size() + " elements)");
        } catch (IOException e) {
            LoggerUtil.error("[Locators] Failed to parse: " + filePath + " — " + e.getMessage());
        }

        return result;
    }

    /**
     * Parses expression: "type::value | type::value | type::value"
     * Returns list of LocatorStrategy in priority order (left to right).
     *
     * Example:
     *   "xpath:://div[@id='cart'] | css::#cart | id::cart_icon"
     *   → [LocatorStrategy(xpath, //div[@id='cart']),
     *      LocatorStrategy(css, #cart),
     *      LocatorStrategy(id, cart_icon)]
     */
    private List<LocatorStrategy> parseExpression(String expression, String elementName) {
        List<LocatorStrategy> strategies = new ArrayList<>();

        // Split by pipe (|) for OR conditions
        String[] parts = expression.split("\\|");

        for (String part : parts) {
            part = part.trim();
            Matcher matcher = LOCATOR_PATTERN.matcher(part);

            if (matcher.matches()) {
                String type  = matcher.group(1).trim().toLowerCase();
                String value = matcher.group(2).trim();
                strategies.add(new LocatorStrategy(type, value));
            } else {
                LoggerUtil.warn("[Locators] Invalid expression format for '" + elementName +
                        "': " + part + " (expected: type::value)");
            }
        }

        if (strategies.isEmpty()) {
            LoggerUtil.warn("[Locators] No valid strategies parsed for: " + elementName);
        }

        return strategies;
    }

    private By buildBy(String type, String value, String elementName) {
        if (type == null || value == null) {
            throw new LocatorNotFoundException("unknown", elementName + " (missing type or value)");
        }
        return switch (type.toLowerCase()) {
            case "id"            -> By.id(value);
            case "xpath"         -> By.xpath(value);
            case "css"           -> By.cssSelector(value);
            case "accessibility" -> By.xpath("//*[@content-desc='" + value + "' or @name='" + value + "']");
            case "classname"     -> By.className(value);
            case "name"          -> By.name(value);
            case "tagname"       -> By.tagName(value);
            default -> throw new LocatorNotFoundException("unknown",
                    elementName + " (unsupported type: " + type + ")");
        };
    }

    private int countLeadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    // ── Value Objects ─────────────────────────────────────────

    /**
     * Represents a single locator strategy: type + value.
     * Used for OR conditions in YAML.
     */
    public static class LocatorStrategy {
        public final String type;
        public final String value;

        public LocatorStrategy(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public By toBy(String elementName) {
            return switch (type.toLowerCase()) {
                case "id"            -> By.id(value);
                case "xpath"         -> By.xpath(value);
                case "css"           -> By.cssSelector(value);
                case "accessibility" -> By.xpath("//*[@content-desc='" + value + "' or @name='" + value + "']");
                case "classname"     -> By.className(value);
                case "name"          -> By.name(value);
                case "tagname"       -> By.tagName(value);
                default -> throw new LocatorNotFoundException("unknown",
                        elementName + " (unsupported type: " + type + ")");
            };
        }

        @Override
        public String toString() {
            return type + "::" + value;
        }
    }
}
