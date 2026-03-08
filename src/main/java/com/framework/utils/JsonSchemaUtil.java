package com.framework.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * JsonSchemaUtil - Reads JSON request/response schema files from the classpath.
 *
 * Files are stored under: src/main/resources/api-schemas/
 *
 * Usage:
 *   String body = JsonSchemaUtil.read("CreateUser.json");
 *   Response response = RestApiClient.post(baseUrl, "/users", body);
 *
 * With placeholder replacement:
 *   String body = JsonSchemaUtil.read("CreateUser.json", "John", "john@test.com");
 *   // CreateUser.json contains %s placeholders that get replaced sequentially
 *
 * Thread-safe: stateless utility, no shared state.
 */
public final class JsonSchemaUtil {

    private static final String BASE_PATH = "api-schemas/";

    private JsonSchemaUtil() {}

    /**
     * Reads a JSON file from api-schemas/ and returns it as a String.
     *
     * @param fileName the JSON file name (e.g., "CreateUser.json")
     * @return file content as String
     * @throws IllegalArgumentException if the file is not found
     */
    public static String read(String fileName) {
        String path = BASE_PATH + fileName;

        try (InputStream is = JsonSchemaUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("JSON schema not found: " + path);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LoggerUtil.debug("[JsonSchema] Loaded: " + path + " (" + content.length() + " chars)");
            return content;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON schema: " + path, e);
        }
    }

    /**
     * Reads a JSON file and replaces %s placeholders with the given values.
     *
     * Example:
     *   CreateUser.json: { "name": "%s", "email": "%s" }
     *   JsonSchemaUtil.read("CreateUser.json", "John", "john@test.com")
     *   → { "name": "John", "email": "john@test.com" }
     *
     * @param fileName     the JSON file name
     * @param replacements values to substitute for %s placeholders (sequential)
     * @return file content with placeholders replaced
     */
    public static String read(String fileName, String... replacements) {
        String content = read(fileName);
        if (replacements == null || replacements.length == 0) {
            return content;
        }
        return String.format(content, (Object[]) replacements);
    }
}

