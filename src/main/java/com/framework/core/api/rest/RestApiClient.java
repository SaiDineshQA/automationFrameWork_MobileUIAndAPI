package com.framework.core.api.rest;

import com.framework.core.config.ConfigManager;
import com.framework.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * RestApiClient - Fluent REST API automation with RestAssured.
 * <p>
 * Features:
 * - Fluent builder pattern
 * - Automatic base URI from config
 * - Request/response logging
 * - Token/auth header management
 * - JSON/XML/form-data support
 * - Response validation helpers
 * <p>
 * Thread-safe: each instance is independent.
 */
public class RestApiClient {

    private final RequestSpecBuilder specBuilder;
    private String endpoint;
    private Object body;

    static {
        // Global RestAssured config
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public RestApiClient() {
        this.specBuilder = new RequestSpecBuilder();

        // Set base URI from config
        String baseUri = ConfigManager.get("api.base.url", "");
        if (!baseUri.isEmpty()) {
            specBuilder.setBaseUri(baseUri);
        }

        // Default content type
        specBuilder.setContentType(ContentType.JSON);
        specBuilder.setAccept(ContentType.JSON);
    }

    /**
     * Set base URI (overrides config).
     */
    public RestApiClient baseUri(String uri) {
        specBuilder.setBaseUri(uri);
        return this;
    }

    /**
     * Set endpoint path.
     */
    public RestApiClient endpoint(String path) {
        this.endpoint = path;
        return this;
    }

    /**
     * Add header.
     */
    public RestApiClient header(String key, String value) {
        specBuilder.addHeader(key, value);
        return this;
    }

    /**
     * Add multiple headers.
     */
    public RestApiClient headers(Map<String, String> headers) {
        specBuilder.addHeaders(headers);
        return this;
    }

    /**
     * Add bearer token authentication.
     */
    public RestApiClient bearerAuth(String token) {
        specBuilder.addHeader("Authorization", "Bearer " + token);
        return this;
    }

    /**
     * Add basic authentication.
     */
    public RestApiClient basicAuth(String username, String password) {
        specBuilder.setAuth(RestAssured.basic(username, password));
        return this;
    }

    /**
     * Add query parameter.
     */
    public RestApiClient queryParam(String key, Object value) {
        specBuilder.addQueryParam(key, value);
        return this;
    }

    /**
     * Add multiple query parameters.
     */
    public RestApiClient queryParams(Map<String, ?> params) {
        specBuilder.addQueryParams(params);
        return this;
    }

    /**
     * Add path parameter.
     */
    public RestApiClient pathParam(String key, Object value) {
        specBuilder.addPathParam(key, value);
        return this;
    }

    /**
     * Set request body (JSON object, Map, or String).
     */
    public RestApiClient body(Object bodyContent) {
        this.body = bodyContent;
        return this;
    }

    /**
     * Set content type.
     */
    public RestApiClient contentType(ContentType type) {
        specBuilder.setContentType(type);
        return this;
    }

    /**
     * Set content type to XML.
     */
    public RestApiClient asXml() {
        specBuilder.setContentType(ContentType.XML);
        specBuilder.setAccept(ContentType.XML);
        return this;
    }

    /**
     * Set content type to form data.
     */
    public RestApiClient asFormData() {
        specBuilder.setContentType(ContentType.URLENC);
        return this;
    }

    // ── HTTP Methods ──────────────────────────────────────────

    public Response get() {
        return execute("GET");
    }

    public Response post() {
        return execute("POST");
    }

    public Response put() {
        return execute("PUT");
    }

    public Response patch() {
        return execute("PATCH");
    }

    public Response delete() {
        return execute("DELETE");
    }

    private Response execute(String method) {
        RequestSpecification spec = specBuilder.build();

        if (body != null) {
            spec.body(body);
        }

        LoggerUtil.info("[API] " + method + " " + endpoint);

        Response response = switch (method.toUpperCase()) {
            case "GET" -> RestAssured.given(spec).get(endpoint);
            case "POST" -> RestAssured.given(spec).post(endpoint);
            case "PUT" -> RestAssured.given(spec).put(endpoint);
            case "PATCH" -> RestAssured.given(spec).patch(endpoint);
            case "DELETE" -> RestAssured.given(spec).delete(endpoint);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };

        LoggerUtil.info("[API] Response: " + response.getStatusCode() +
                " | Time: " + response.getTime() + "ms");

        return response;
    }

    // ── Static Helpers ────────────────────────────────────────
    //
    // Clean one-liner API calls:
    //   RestApiClient.get(baseUrl, path)
    //   RestApiClient.post(baseUrl, path, body)
    //   RestApiClient.put(baseUrl, path, body)
    //   RestApiClient.patch(baseUrl, path, body)
    //   RestApiClient.delete(baseUrl, path)
    //
    // With optional headers/query params/auth:
    //   RestApiClient.get(baseUrl, path, headers)
    //   RestApiClient.get(baseUrl, path, headers, queryParams)
    //

    /**
     * GET request.
     */
    public static Response get(String baseUrl, String path) {
        return new RestApiClient().baseUri(baseUrl).endpoint(path).execute("GET");
    }


    /**
     * GET request with query params.
     */
    public static Response get(String baseUrl, String path,
                               Map<String, ?> queryParams) {
        return new RestApiClient().baseUri(baseUrl).endpoint(path)
                .queryParams(queryParams).execute("GET");
    }

    /**
     * POST request with body.
     */
    public static Response post(String baseUrl, String path, Object body) {
        return new RestApiClient().baseUri(baseUrl).endpoint(path).body(body).execute("POST");
    }


    /**
     * PUT request with body.
     */
    public static Response put(String baseUrl, String path, Object body) {
        return new RestApiClient().baseUri(baseUrl).endpoint(path).body(body).execute("PUT");
    }


    /**
     * PATCH request with body.
     */
    public static Response patch(String baseUrl, String path, Object body) {
        return new RestApiClient().baseUri(baseUrl).endpoint(path).body(body).execute("PATCH");
    }


    /**
     * DELETE request.
     */
    public static Response delete(String baseUrl, String path) {
        return new RestApiClient().baseUri(baseUrl).endpoint(path).execute("DELETE");
    }

}
