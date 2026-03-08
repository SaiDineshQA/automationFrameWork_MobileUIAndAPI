package com.framework.pages.api;

import com.framework.core.interfaces.IReporter;
import com.framework.core.reports.extent.ExtentReporter;
import com.framework.utils.LoggerUtil;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;

/**
 * BaseApiPage - Base class for all API page objects.
 *
 * Wraps a RestAssured Response and provides report-aware assertions
 * driven by enum-based JSON paths.
 *
 * Each API page enum overrides toString() to return the JSON path.
 * BaseApiPage methods accept Enum<?> and call toString() to resolve the path.
 *
 * Every assertion is logged to the Extent Report automatically.
 */
public abstract class BaseApiPage {

    protected Response response;
    private static final IReporter reporter = new ExtentReporter();

    /**
     * Set the response to validate.
     *
     *   getPostApi.setResponse(response);
     *   String title = getPostApi.getString(GetAccountDetailsApi.Fields.TITLE);
     */
    public void setResponse(Response response) {
        this.response = response;
    }

    /**
     * Returns the API page name for logging (auto-derived from class name).
     */
    protected String getApiName() {
        return this.getClass().getSimpleName();
    }

    // ── Assertions (each one logs to report) ─────────────────

    /**
     * Assert a field equals the expected value.
     *
     *   assertField(Fields.NAME, "Leanne Graham")
     *   → extracts response.jsonPath().getString("name")
     *   → asserts equals "Leanne Graham"
     *   → logs: ✓ GetUserInfoApi.NAME = Leanne Graham
     */
    public BaseApiPage assertField(Enum<?> jsonPath, Object expectedValue) {
        String path = jsonPath.toString();
        Object actual = response.jsonPath().get(path);
        boolean passed = equals(actual, expectedValue);

        String msg = String.format("%s.%s | path: %s | expected: <%s> | actual: <%s>",
                getApiName(), jsonPath.name(), path, expectedValue, actual);

        if (passed) {
            logPass("✓ " + msg);
        } else {
            logFail("✗ " + msg);
            Assert.fail("Assertion failed → " + msg);
        }
        return this;
    }

    /**
     * Assert a field is not null / not empty.
     */
    public BaseApiPage assertNotNull(Enum<?> jsonPath) {
        String path = jsonPath.toString();
        Object actual = response.jsonPath().get(path);
        String msg = String.format("%s.%s | path: %s", getApiName(), jsonPath.name(), path);

        if (actual != null && !actual.toString().isEmpty()) {
            logPass("✓ " + msg + " is not null → " + actual);
        } else {
            logFail("✗ " + msg + " is null or empty");
            Assert.fail("Field is null → " + msg);
        }
        return this;
    }

    /**
     * Assert a field is null.
     */
    public BaseApiPage assertNull(Enum<?> jsonPath) {
        String path = jsonPath.toString();
        Object actual = response.jsonPath().get(path);
        String msg = String.format("%s.%s | path: %s", getApiName(), jsonPath.name(), path);

        if (actual == null || actual.toString().isEmpty()) {
            logPass("✓ " + msg + " is null as expected");
        } else {
            logFail("✗ " + msg + " expected null but got: " + actual);
            Assert.fail("Field is not null → " + msg);
        }
        return this;
    }

    /**
     * Assert a string field contains a substring.
     */
    public BaseApiPage assertContains(Enum<?> jsonPath, String substring) {
        String path = jsonPath.toString();
        String actual = response.jsonPath().getString(path);
        String msg = String.format("%s.%s | path: %s | contains: '%s' | actual: <%s>",
                getApiName(), jsonPath.name(), path, substring, actual);

        if (actual != null && actual.contains(substring)) {
            logPass("✓ " + msg);
        } else {
            logFail("✗ " + msg);
            Assert.fail("Assertion failed → " + msg);
        }
        return this;
    }

    /**
     * Assert a list field has expected size.
     */
    public BaseApiPage assertListSize(Enum<?> jsonPath, int expectedSize) {
        String path = jsonPath.toString();
        List<?> actual = response.jsonPath().getList(path);
        int actualSize = actual == null ? 0 : actual.size();
        String msg = String.format("%s.%s | path: %s | expected size: %d | actual size: %d",
                getApiName(), jsonPath.name(), path, expectedSize, actualSize);

        if (actualSize == expectedSize) {
            logPass("✓ " + msg);
        } else {
            logFail("✗ " + msg);
            Assert.fail("Assertion failed → " + msg);
        }
        return this;
    }

    /**
     * Assert HTTP status code.
     */
    public BaseApiPage assertStatusCode(int expectedCode) {
        int actual = response.getStatusCode();
        String msg = String.format("%s | expected status: %d | actual: %d",
                getApiName(), expectedCode, actual);

        if (actual == expectedCode) {
            logPass("✓ " + msg);
        } else {
            logFail("✗ " + msg);
            Assert.fail("Status code mismatch → " + msg);
        }
        return this;
    }

    /**
     * Assert response time is under a threshold.
     */
    public BaseApiPage assertResponseTimeUnder(long maxMillis) {
        long actual = response.getTime();
        String msg = String.format("%s | expected < %dms | actual: %dms",
                getApiName(), maxMillis, actual);

        if (actual <= maxMillis) {
            logPass("✓ " + msg);
        } else {
            logFail("✗ " + msg);
            Assert.fail("Response too slow → " + msg);
        }
        return this;
    }

    // ── Value getters (use with standard Assert) ───────────

    /**
     * Resolves a field name to its JSON path by looking up the Fields enum in the concrete page class.
     *
     *   getAccountDetailsApi.getJsonPath("USER_ID")  → "userId"
     *   getUserInfoApi.getJsonPath("ADDRESS_CITY")    → "address.city"
     *
     * @param fieldName the enum constant name (e.g., "USER_ID", "TITLE")
     * @return the JSON path string from the enum's toString()
     * @throws IllegalArgumentException if the field name is not found in the Fields enum
     */
    public String getJsonPath(String fieldName) {
        for (Class<?> inner : this.getClass().getDeclaredClasses()) {
            if (inner.isEnum() && inner.getSimpleName().equals("Fields")) {
                for (Object constant : inner.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals(fieldName)) {
                        return constant.toString();
                    }
                }
                throw new IllegalArgumentException(
                        getApiName() + " has no Fields enum constant named '" + fieldName + "'");
            }
        }
        throw new IllegalArgumentException(getApiName() + " does not have a Fields enum");
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Enum<?> jsonPath) {
        return (T) response.jsonPath().get(jsonPath.toString());
    }

    public String getString(Enum<?> jsonPath) {
        return response.jsonPath().getString(jsonPath.toString());
    }

    public int getInt(Enum<?> jsonPath) {
        return response.jsonPath().getInt(jsonPath.toString());
    }

    public long getLong(Enum<?> jsonPath) {
        return response.jsonPath().getLong(jsonPath.toString());
    }

    public boolean getBoolean(Enum<?> jsonPath) {
        return response.jsonPath().getBoolean(jsonPath.toString());
    }

    public <T> List<T> getList(Enum<?> jsonPath) {
        return response.jsonPath().getList(jsonPath.toString());
    }

    // ── String-based getters (resolve field name → JSON path via Fields enum) ──

    public String getString(String fieldName) {
        return response.jsonPath().getString(getJsonPath(fieldName));
    }

    public int getInt(String fieldName) {
        return response.jsonPath().getInt(getJsonPath(fieldName));
    }

    public long getLong(String fieldName) {
        return response.jsonPath().getLong(getJsonPath(fieldName));
    }

    public boolean getBoolean(String fieldName) {
        return response.jsonPath().getBoolean(getJsonPath(fieldName));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String fieldName) {
        return (T) response.jsonPath().get(getJsonPath(fieldName));
    }

    public <T> List<T> getList(String fieldName) {
        return response.jsonPath().getList(getJsonPath(fieldName));
    }

    public int getStatusCode() {
        return response.getStatusCode();
    }

    public Response getResponse() {
        return response;
    }

    // ── Internal helpers ─────────────────────────────────────

    private boolean equals(Object actual, Object expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        if (actual instanceof Number && expected instanceof Number) {
            return ((Number) actual).doubleValue() == ((Number) expected).doubleValue();
        }
        return actual.equals(expected) || actual.toString().equals(expected.toString());
    }

    private void logPass(String msg) {
        LoggerUtil.info("[API] " + msg);
        reporter.logStep(msg);
    }

    private void logFail(String msg) {
        LoggerUtil.error("[API] " + msg);
        reporter.logStep(msg);
    }
}
