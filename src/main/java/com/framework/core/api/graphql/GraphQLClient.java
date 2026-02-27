package com.framework.core.api.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.core.config.ConfigManager;
import com.framework.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * GraphQLClient - Fluent GraphQL automation.
 *
 * Features:
 * - Query and mutation support
 * - Variables support
 * - Fragments support
 * - Error handling
 * - Response data extraction
 * - Bearer token auth
 *
 * Thread-safe: each instance is independent.
 */
public class GraphQLClient {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint;
    private final Map<String, String> headers;
    private String query;
    private Map<String, Object> variables;

    public GraphQLClient() {
        this.endpoint = ConfigManager.get("graphql.endpoint", "");
        this.headers  = new HashMap<>();
        this.variables = new HashMap<>();
    }

    public GraphQLClient(String endpoint) {
        this.endpoint = endpoint;
        this.headers  = new HashMap<>();
        this.variables = new HashMap<>();
    }

    /**
     * Set GraphQL query or mutation.
     */
    public GraphQLClient query(String graphqlQuery) {
        this.query = graphqlQuery;
        return this;
    }

    /**
     * Add a variable.
     */
    public GraphQLClient variable(String key, Object value) {
        this.variables.put(key, value);
        return this;
    }

    /**
     * Add multiple variables.
     */
    public GraphQLClient variables(Map<String, Object> vars) {
        this.variables.putAll(vars);
        return this;
    }

    /**
     * Add header.
     */
    public GraphQLClient header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Add bearer token authentication.
     */
    public GraphQLClient bearerAuth(String token) {
        this.headers.put("Authorization", "Bearer " + token);
        return this;
    }

    /**
     * Execute the GraphQL request.
     */
    public GraphQLResponse execute() {
        if (query == null || query.isEmpty()) {
            throw new IllegalStateException("GraphQL query not set. Call query() first.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);

        if (!variables.isEmpty()) {
            payload.put("variables", variables);
        }

        LoggerUtil.info("[GraphQL] Executing query to: " + endpoint);
        LoggerUtil.debug("[GraphQL] Query: " + query);
        LoggerUtil.debug("[GraphQL] Variables: " + variables);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .headers(headers)
                .body(payload)
                .post(endpoint);

        LoggerUtil.info("[GraphQL] Response: " + response.getStatusCode() +
                " | Time: " + response.getTime() + "ms");

        return new GraphQLResponse(response);
    }

    // ── Static Helpers ────────────────────────────────────────

    /**
     * Quick query execution.
     */
    public static GraphQLResponse query(String endpoint, String query) {
        return new GraphQLClient(endpoint).query(query).execute();
    }

    /**
     * Query with variables.
     */
    public static GraphQLResponse query(String endpoint, String query, Map<String, Object> variables) {
        return new GraphQLClient(endpoint).query(query).variables(variables).execute();
    }

    // ── Response Wrapper ──────────────────────────────────────

    public static class GraphQLResponse {
        private final Response rawResponse;
        private JsonNode rootNode;
        private JsonNode dataNode;
        private JsonNode errorsNode;

        public GraphQLResponse(Response response) {
            this.rawResponse = response;
            parseResponse();
        }

        private void parseResponse() {
            try {
                this.rootNode = mapper.readTree(rawResponse.getBody().asString());
                this.dataNode = rootNode.path("data");
                this.errorsNode = rootNode.path("errors");
            } catch (Exception e) {
                LoggerUtil.error("[GraphQL] Failed to parse response: " + e.getMessage());
            }
        }

        /**
         * Get data node.
         */
        public JsonNode getData() {
            return dataNode;
        }

        /**
         * Get data at specific path.
         * Example: getData("user.profile.email")
         */
        public JsonNode getData(String path) {
            String[] parts = path.split("\\.");
            JsonNode current = dataNode;
            for (String part : parts) {
                current = current.path(part);
            }
            return current;
        }

        /**
         * Get data as specific type.
         */
        public <T> T getDataAs(Class<T> type) {
            try {
                return mapper.treeToValue(dataNode, type);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert data to " + type.getSimpleName(), e);
            }
        }

        /**
         * Check if response has errors.
         */
        public boolean hasErrors() {
            return errorsNode != null && !errorsNode.isMissingNode() && !errorsNode.isEmpty();
        }

        /**
         * Get errors array.
         */
        public JsonNode getErrors() {
            return errorsNode;
        }

        /**
         * Get first error message.
         */
        public String getFirstErrorMessage() {
            if (hasErrors() && errorsNode.isArray() && errorsNode.size() > 0) {
                return errorsNode.get(0).path("message").asText();
            }
            return "";
        }

        /**
         * Get HTTP status code.
         */
        public int getStatusCode() {
            return rawResponse.getStatusCode();
        }

        /**
         * Get response time in milliseconds.
         */
        public long getTime() {
            return rawResponse.getTime();
        }

        /**
         * Get raw RestAssured response.
         */
        public Response getRawResponse() {
            return rawResponse;
        }

        /**
         * Get full response body as string.
         */
        public String getBody() {
            return rawResponse.getBody().asString();
        }

        /**
         * Pretty print response.
         */
        public void prettyPrint() {
            rawResponse.prettyPrint();
        }

        /**
         * Assert no errors.
         */
        public GraphQLResponse assertNoErrors() {
            if (hasErrors()) {
                throw new AssertionError("GraphQL response contains errors: " +
                        errorsNode.toString());
            }
            return this;
        }

        /**
         * Assert status code.
         */
        public GraphQLResponse assertStatusCode(int expected) {
            if (getStatusCode() != expected) {
                throw new AssertionError("Expected status " + expected +
                        " but got " + getStatusCode());
            }
            return this;
        }
    }
}
